package xyz.daikirai.gui.opai;

import java.util.*;
import static xyz.daikirai.gui.opai.OpaiMath.*;

/** Stateful ClickGUI; rendered layout and input share the same animated clipping coordinates. */
public final class OpaiUi {
    public static final float COL_W=152, GAP=8, HEADER=21, ROW=19.6f, FOOTER=7;
    public interface Feature {
        String name(); boolean enabled(); void toggle(); boolean bindable(); void bind(int key);
        List<Control> controls();
    }
    public interface Control {
        String name(); Kind kind(); double value(); double min(); double max(); String display(); String[] options();
        void set(double value);
    }
    public interface Configurations {
        List<String> names(); void refresh(); void load(String name); void save(String name); void delete(String name);
        void create(String name,boolean blank); void folder();
    }
    public enum Kind { BOOLEAN, NUMBER, MODE, ACTION, LABEL }
    public static final class Column {
        public final String name;
        public final List<Feature> features=new ArrayList<>();
        float x,y,w=COL_W,body,content,scroll,targetScroll,maxScroll;
        boolean expanded=true;
        final OpaiMotion expand=new OpaiMotion(1,40);
        final List<Entry> entries=new ArrayList<>();
        Column(String name) { this.name=name; }
    }
    private static final class FeatureState {
        boolean expanded;
        final OpaiMotion reveal=new OpaiMotion(0,40),enabled,hover=new OpaiMotion(0,30);
        final OpaiFeedback ink=new OpaiFeedback();
        FeatureState(Feature f) { enabled=new OpaiMotion(f.enabled()?1:0,30); }
    }
    private static final class ControlState {
        final OpaiMotion value,dropdown=new OpaiMotion(0,40);
        final OpaiFeedback ink=new OpaiFeedback();
        boolean open; float scroll;
        ControlState(Control control) { value=new OpaiMotion(fraction(control),control.kind()==Kind.BOOLEAN?30:35); }
    }
    private static final class Entry {
        final Feature feature; final Control control; final float top,height,bottom;
        Entry(Feature f,Control control,float top,float height,float bottom) { this.feature=f;this.control=control;this.top=top;this.height=height;this.bottom=bottom; }
    }
    private static final class Dropdown {
        final Column col; final Control control; final float x,y,w,h,full;
        Dropdown(Column col,Control control,float x,float y,float w,float h,float full) { this.col=col;this.control=control;this.x=x;this.y=y;this.w=w;this.h=h;this.full=full; }
        boolean contains(float mx,float my) { return hit(mx,my,x,y,w,h); }
    }
    public final List<Column> columns=new ArrayList<>();
    private final Map<Feature,FeatureState> featureStates=new IdentityHashMap<>();
    private final Map<Control,ControlState> controlStates=new IdentityHashMap<>();
    private final OpaiConfigurations configs;
    private final List<Dropdown> dropdowns=new ArrayList<>();
    private final OpaiMotion opacity=new OpaiMotion(0,30);
    private Column dragged,scrollbar;
    private Control slider;
    private Column sliderColumn;
    private Feature binding;
    private float dragX,dragY,grab,width,height,scale=1,centerY,mx,my,horizontal,maxHorizontal;
    private double opened,last,closingAt;
    private float closingAlpha,closingScale;
    private boolean closing;
    private Runnable closeAction=()->{};
    private Runnable editAction=()->{};
    private boolean pencil;

    public OpaiUi(Configurations backend) {
        for(String name:new String[]{"Combat","Movement","Player","Visual","Misc"}) columns.add(new Column(name));
        configs=new OpaiConfigurations(backend);
        configs.settings=this::showSettings;
        configs.edit=()->editAction.run();
    }
    public void actions(Runnable close,Runnable edit) { closeAction=close;editAction=edit; }
    public void pencil(boolean visible) { pencil=visible; }
    public void open(float width,float height,double now) {
        boolean first=this.width==0;
        resize(width,height,first);
        opened=last=now; closing=false; opacity.snap(0,now);opacity.approach(1,now);
        configs.collapse(); configs.refresh();
    }
    public void resize(float width,float height,boolean reset) {
        this.width=width;this.height=height;
        float margin=Math.min(30,width*.0382f),top=Math.max(8,height*.112f);
        float w=Math.max(1,Math.min(COL_W,width-margin*2));
        maxHorizontal=Math.max(0,margin*2+columns.size()*w+(columns.size()-1)*GAP-width);
        if(reset) {
            horizontal=0;
            for(int i=0;i<columns.size();i++) { Column col=columns.get(i);col.x=margin+i*(w+GAP);col.y=top; }
        }
        for(Column col:columns) { col.w=w;col.y=clamp(col.y,4,Math.max(4,height-HEADER-ROW-8)); }
        float maxBody=Math.min(16*ROW+FOOTER,Math.max(ROW,height-top-HEADER-Math.max(8,height*.10f)));
        centerY=top+(HEADER+maxBody)/2;
        configs.dock(width-margin,top-2,w,height);
    }
    public void mouse(float x,float y) { mx=width/2+(x-width/2)/scale;my=centerY+(y-centerY)/scale; }
    public void paint(OpaiCanvas c,OpaiStyle.Palette p,float mouseX,float mouseY,double now) {
        float alpha=closing?closingAlpha*(float)Math.exp(-30*(now-closingAt)/1000):opacity.approach(1,now);
        scale=closing?closingScale+.6f*(1-alpha/Math.max(.001f,closingAlpha)):OpaiMotion.openingScale(now-opened);
        mouse(mouseX,mouseY);
        float smoothing=1-(float)Math.exp(-Math.max(0,now-last)*.014);last=now;
        if(closing && alpha<=.01f) { closeAction.run();return; }
        if(dragged!=null) {
            dragged.x=clamp(mx-dragX,4-maxHorizontal,Math.max(4,width-dragged.w-4+maxHorizontal));
            dragged.y=clamp(my-dragY,4,Math.max(4,height-HEADER-dragged.body-8));
        }
        if(slider!=null) { slider.set(slider.min()+(slider.max()-slider.min())*clamp((mx-sliderColumn.x-10)/(sliderColumn.w-20),0,1)); state(slider).value.snap(fraction(slider),now); }
        dropdowns.clear();
        for(Column col:columns) {
            if(scrollbar==col) updateScrollbar(col);
            col.scroll+=(col.targetScroll-col.scroll)*smoothing;
            layout(col,now);
        }
        configs.update(now);
        c.opacity(alpha,()->c.scale(scale,width/2,centerY,()-> {
            for(Column col:columns) paintColumn(c,p,col,now);
            configs.paint(c,p,mx,my,now);
        }));
        if(pencil) {
            float y=height-8-26.7f+34.7f*(closing?1-alpha:(float)Math.exp(-(now-opened)/1000*30));
            c.opacity(alpha,()->OpaiWidgets.pencil(c,width-34.7f,y,p.accent()));
        }
    }
    private FeatureState state(Feature f) { return featureStates.computeIfAbsent(f,FeatureState::new); }
    private ControlState state(Control control) { return controlStates.computeIfAbsent(control,ControlState::new); }
    private static float fraction(Control control) {
        if(control.kind()==Kind.BOOLEAN) return control.value()!=0?1:0;
        return control.max()<=control.min()?0:(float)clamp((control.value()-control.min())/(control.max()-control.min()),0,1);
    }
    private static float controlHeight(Control control) { return control.kind()==Kind.MODE?40:control.kind()==Kind.NUMBER?20:ROW; }
    private void layout(Column col,double now) {
        col.entries.clear();float top=0;
        for(Feature f:col.features) {
            col.entries.add(new Entry(f,null,top,ROW,top+ROW));top+=ROW;
            FeatureState fs=state(f);float progress=fs.reveal.approach(fs.expanded?1:0,now);
            float full=0;for(Control control:f.controls()) full+=controlHeight(control);
            float bottom=top+full*progress, controlY=top;
            for(Control control:f.controls()) {
                float h=controlHeight(control);ControlState cs=state(control);
                cs.dropdown.approach(cs.open && fs.expanded && col.expanded?1:0,now);
                if(progress>0 && bottom>controlY) col.entries.add(new Entry(f,control,controlY,h,Math.min(controlY+h,bottom)));
                controlY+=h;
            }
            top=bottom;
        }
        col.content=top;
        float max=Math.min(16*ROW+FOOTER,Math.max(ROW,height-col.y-HEADER-8));
        float full=Math.min(max,top+FOOTER);
        col.body=full*col.expand.approach(col.expanded?1:0,now);
        col.maxScroll=Math.max(0,top-(full-FOOTER));col.targetScroll=clamp(col.targetScroll,0,col.maxScroll);
        if(!col.expanded && col.body<.5f) col.scroll=col.targetScroll=0;
        for(Entry entry:col.entries) {
            if(entry.control==null || entry.control.kind()!=Kind.MODE || state(entry.control).dropdown.value()<=0) continue;
            Control control=entry.control;ControlState cs=state(control);
            float y=col.y+HEADER+entry.top-col.scroll+37;
            if(y<col.y+HEADER || y>col.y+HEADER+col.body-FOOTER) continue;
            float fullHeight=Math.min(control.options().length*20,Math.max(20,height-y-8));
            cs.scroll=clamp(cs.scroll,0,Math.max(0,control.options().length*20-fullHeight));
            dropdowns.add(new Dropdown(col,control,col.x+8,y,col.w-16,fullHeight*cs.dropdown.value(),fullHeight));
        }
    }
    private void paintColumn(OpaiCanvas c,OpaiStyle.Palette p,Column col,double now) {
        if(col.body<=.5f) c.rounded(col.x,col.y,col.w,HEADER,7,p.header());
        else { c.panel(col.x,col.y+HEADER,col.w,col.body,false,p.body());c.panel(col.x,col.y,col.w,HEADER,true,p.header()); }
        c.text(col.name,col.x+10,col.y+HEADER/2,9.2f,col.w-20,p.text());
        if(col.body>FOOTER+.5f) c.clip(col.x+1,col.y+HEADER,col.w-2,col.body-FOOTER,()-> {
            for(Entry entry:col.entries) {
                float y=col.y+HEADER+entry.top-col.scroll;
                if(y+entry.bottom-entry.top<=col.y+HEADER || y>=col.y+HEADER+col.body-FOOTER) continue;
                c.clip(col.x,y,col.w,entry.bottom-entry.top,()->paintEntry(c,p,col,entry,y,now));
            }
        });
        if(col.maxScroll>0 && col.body>FOOTER) {
            float visible=col.body-FOOTER,track=Math.max(0,visible-4),thumb=Math.min(track,Math.max(12,track*visible/Math.max(1,col.content)));
            float y=col.y+HEADER+2+(track-thumb)*col.scroll/col.maxScroll;
            c.rounded(col.x+col.w-3.5f,y,2.5f,thumb,1.25f,p.scrollbar());
        }
        for(Dropdown menu:dropdowns) if(menu.col==col) paintDropdown(c,p,menu,now);
    }
    private boolean unobstructed(Column col) { return !configs.contains(mx,my) && columnAt(mx,my)==col && dropdownAt(mx,my)==null; }
    private void paintEntry(OpaiCanvas c,OpaiStyle.Palette p,Column col,Entry entry,float y,double now) {
        float x=col.x,w=col.w;
        boolean hovered=unobstructed(col) && hit(mx,my,x,y,w,entry.bottom-entry.top);
        if(entry.control==null) {
            FeatureState s=state(entry.feature);
            float enabled=s.enabled.approach(entry.feature.enabled()?1:0,now),hover=s.hover.approach(hovered && dragged==null?1:0,now);
            if(hover>.01f && enabled<.99f) c.rect(x,y,w,ROW,alpha(p.hover(),hover*(1-enabled)));
            if(enabled>.01f) c.rect(x,y,w,ROW,alpha(mix(p.enabled(),p.enabledHover(),hover),enabled));
            s.ink.paint(c,x,y,w,ROW,0,p.text(),now);
            c.text(binding==entry.feature?"Press a key":display(entry.feature.name()),x+10,y+ROW/2,8.3f,w-20,mix(p.text(),p.enabledText(),enabled));return;
        }
        Control control=entry.control;ControlState s=state(control);
        switch(control.kind()) {
            case BOOLEAN:
                c.text(display(control.name()),x+10,y+ROW/2,8.5f,w-50,p.text());
                OpaiWidgets.toggle(c,p,x+w-36,y+(ROW-16)/2,s.value.approach(control.value()!=0?1:0,now),s.ink.press(now));
                s.ink.paint(c,x+w-36,y+(ROW-16)/2,26,16,8,p.accent(),now);break;
            case NUMBER:
                String value=control.display();float valueWidth=Math.min((w-20)*.45f,c.measure(value,8.5f));
                c.text(display(control.name()),x+10,y+6.5f,8.5f,w-24-valueWidth,p.text());
                c.text(value,x+w-10-valueWidth,y+6.5f,8.5f,valueWidth,p.text());
                OpaiWidgets.slider(c,p,x,y,w,s.value.approach(fraction(control),now),s.ink.press(now));break;
            case MODE:
                c.text(display(control.name()),x+10,y+8,8.5f,w-20,p.text());
                OpaiWidgets.field(c,p,x,y,w,display(control.display()),s.open || s.dropdown.value()>0);
                s.ink.paint(c,x+8,y+16,w-16,21,2,p.accent(),now);break;
            case ACTION:
                c.rounded(x+8,y+2,w-16,ROW-4,(ROW-4)/2,mix(p.field(),p.accent(),hovered?.25f:.15f));
                c.text(display(control.name()),x+14,y+ROW/2,8.3f,w-28,p.text());
                s.ink.paint(c,x+8,y+2,w-16,ROW-4,7,p.accent(),now);break;
            case LABEL:
                c.plainText(control.name(),x+10,y+ROW/2,7.25f,w-20,p.fieldLine());break;
        }
    }
    private void paintDropdown(OpaiCanvas c,OpaiStyle.Palette p,Dropdown menu,double now) {
        c.rounded(menu.x,menu.y,menu.w,menu.h,2,p.field());ControlState s=state(menu.control);
        c.clip(menu.x,menu.y,menu.w,menu.h,()-> {
            String[] options=menu.control.options();
            for(int i=0;i<options.length;i++) {
                float y=menu.y+i*20-s.scroll;if(y+20<=menu.y || y>=menu.y+menu.h) continue;
                boolean selected=(int)menu.control.value()==i;
                if(selected || hit(mx,my,menu.x,y,menu.w,20)) c.rect(menu.x+1,y,menu.w-2,20,selected?p.selected():p.hover());
                if(selected) c.check(menu.x+5,y+7,6,p.text());
                c.text(display(options[i]),menu.x+(selected?17:6),y+10,8.5f,menu.w-(selected?23:12),p.text());
            }
            if(options.length*20>menu.full) {
                float thumb=Math.max(12,menu.full*menu.full/(options.length*20));
                c.rounded(menu.x+menu.w-3,menu.y+2+(menu.full-thumb-4)*s.scroll/(options.length*20-menu.full),2,thumb,1,p.scrollbar());
            }
        });
    }
    public void click(float rawX,float rawY,int button,double now) {
        mouse(rawX,rawY);if(closing) return;
        if(pencil && button==1 && hit(rawX,rawY,width-34.7f,height-34.7f,26.7f,26.7f)) { editAction.run();return; }
        if(configs.contains(mx,my)) { configs.click(mx,my,button,now);return; }
        configs.blur();Dropdown menu=dropdownAt(mx,my);
        if(menu!=null) {
            if(button==0) {
                int i=(int)((my-menu.y+state(menu.control).scroll)/20);
                if(i>=0 && i<menu.control.options().length) menu.control.set(i);
                closeDropdowns();
            }
            return;
        }
        Control previouslyOpen=null;
        for(Map.Entry<Control,ControlState> item:controlStates.entrySet()) if(item.getValue().open) previouslyOpen=item.getKey();
        closeDropdowns();Column col=columnAt(mx,my);if(col==null) return;
        if(my<col.y+HEADER) {
            if(button==0) { dragged=col;dragX=mx-col.x;dragY=my-col.y;columns.remove(col);columns.add(col); }
            else if(button==1) col.expanded=!col.expanded;
            return;
        }
        if(button==0 && col.maxScroll>0 && mx>=col.x+col.w-6) {
            float visible=col.body-FOOTER,track=Math.max(0,visible-4),thumb=Math.min(track,Math.max(12,track*visible/col.content));
            float top=2+(track-thumb)*col.scroll/col.maxScroll,local=my-col.y-HEADER;
            scrollbar=col;grab=local>=top && local<top+thumb?local-top:thumb/2;updateScrollbar(col);return;
        }
        if(my>=col.y+HEADER+col.body-FOOTER) return;
        float local=my-col.y-HEADER+col.scroll;
        for(Entry entry:col.entries) if(local>=entry.top && local<entry.bottom) {
            float y=col.y+HEADER+entry.top-col.scroll;
            if(entry.control==null) {
                FeatureState s=state(entry.feature);
                if(button==0) { s.ink.click(mx-col.x,my-y,col.w,ROW,now);s.enabled.approach(entry.feature.enabled()?1:0,now);entry.feature.toggle();s.enabled.approach(entry.feature.enabled()?1:0,now); }
                else if(button==1) {s.expanded=!s.expanded;if(s.expanded)col.targetScroll=clamp(Math.max(col.scroll,entry.top+ROW+40-(col.body-FOOTER)),0,col.maxScroll);}
                else if(button==2 && entry.feature.bindable()) binding=entry.feature;
            } else if(button==0) {
                Control control=entry.control;ControlState s=state(control);
                s.ink.click(mx-col.x-8,my-y,col.w-16,entry.height,now);
                if(control.kind()==Kind.BOOLEAN) { s.value.approach(control.value()!=0?1:0,now);control.set(control.value()!=0?0:1);s.value.approach(control.value()!=0?1:0,now); }
                else if(control.kind()==Kind.ACTION) control.set(1);
                else if(control.kind()==Kind.MODE && hit(mx,my,col.x+8,y+16,col.w-16,21)) s.open=previouslyOpen!=control;
                else if(control.kind()==Kind.NUMBER && hit(mx,my,col.x+7,y+9.6f,col.w-14,12)) { slider=control;sliderColumn=col;col.targetScroll=col.scroll; }
            }
            return;
        }
    }
    private void updateScrollbar(Column col) {
        float visible=col.body-FOOTER,track=Math.max(0,visible-4),thumb=Math.min(track,Math.max(12,track*visible/Math.max(1,col.content)));
        col.scroll=col.targetScroll=clamp((my-col.y-HEADER-grab-2)/Math.max(1,track-thumb)*col.maxScroll,0,col.maxScroll);
    }
    public void release() { dragged=null;slider=null;scrollbar=null; }
    public void wheel(float rawX,float rawY,int amount,boolean shift) {
        mouse(rawX,rawY);if(closing || dragged!=null || slider!=null || scrollbar!=null) return;
        if(configs.contains(mx,my)) { configs.wheel(amount);return; }
        Dropdown menu=dropdownAt(mx,my);
        if(menu!=null) { ControlState s=state(menu.control);s.scroll=clamp(s.scroll-amount*40,0,Math.max(0,menu.control.options().length*20-menu.full));return; }
        Column col=columnAt(mx,my);
        if(shift || col==null) {
            float next=clamp(horizontal-amount*ROW*2,0,maxHorizontal),delta=next-horizontal;
            for(Column item:columns) item.x-=delta;horizontal=next;
        } else col.targetScroll=clamp(col.targetScroll-amount*ROW*2,0,col.maxScroll);
    }
    public boolean key(char character,int key,double now) {
        if(closing) return true;
        if(binding!=null) { binding.bind(key==1 || key==14?0:key);binding=null;return true; }
        if(configs.key(character,key)) return true;
        if(key==1) { if(!closeDropdowns()) close(now);return true; }
        return false;
    }
    public void close(double now) { if(closing) return;release();closing=true;closingAt=now;closingAlpha=opacity.value();closingScale=scale; }
    private void showSettings() {
        double now=OpaiMotion.now();
        for(Column col:columns) for(Feature feature:col.features) if(feature.name().equals("Settings")) {
            float delta=col.x+col.w>width-8?col.x+col.w-width+8:col.x<8?col.x-8:0;
            float next=clamp(horizontal+delta,0,maxHorizontal);delta=next-horizontal;
            for(Column item:columns)item.x-=delta;horizontal=next;
            col.expanded=true;state(feature).expanded=true;state(feature).reveal.snap(1,now);layout(col,now);
            for(Entry entry:col.entries) if(entry.feature==feature && entry.control==null) {col.scroll=col.targetScroll=clamp(entry.top,0,col.maxScroll);break;}
            return;
        }
    }
    private boolean closeDropdowns() { boolean changed=false;for(ControlState s:controlStates.values()) if(s.open) { s.open=false;changed=true; }return changed; }
    private Column columnAt(float x,float y) {
        for(int i=columns.size()-1;i>=0;i--) {
            Column col=columns.get(i);
            for(Dropdown menu:dropdowns) if(menu.col==col && menu.contains(x,y)) return col;
            if(hit(x,y,col.x,col.y,col.w,HEADER+col.body)) return col;
        }
        return null;
    }
    private Dropdown dropdownAt(float x,float y) { for(int i=dropdowns.size()-1;i>=0;i--) if(dropdowns.get(i).contains(x,y)) return dropdowns.get(i);return null; }
    static boolean hit(float x,float y,float left,float top,float w,float h) { return x>=left && x<left+w && y>=top && y<top+h; }
}
