package xyz.daikirai.gui.opai;

import java.util.*;
import static xyz.daikirai.gui.opai.OpaiMath.*;
import static xyz.daikirai.gui.opai.OpaiUi.hit;
import static xyz.daikirai.gui.opai.OpaiIcons.Icon;

/** The reference's docked chip and animated configuration popup, backed by B4 profiles. */
final class OpaiConfigurations {
    private final OpaiUi.Configurations backend;
    private final OpaiMotion popup=new OpaiMotion(0,18),heightMotion=new OpaiMotion(0,40),pageFade=new OpaiMotion(1,25),scrollMotion=new OpaiMotion(0,20);
    private final Map<String,OpaiFeedback> feedback=new HashMap<>();
    private final Map<String,OpaiMotion> selections=new HashMap<>();
    private List<String> names=new ArrayList<>();
    private float right,top,width=152,viewportHeight,scroll,body,chipWidth=76;
    private boolean expanded,focused;
    private int page,previousPage;
    private String input="",selected,message="";
    private double messageUntil;
    Runnable settings=()->{},edit=()->{};
    OpaiConfigurations(OpaiUi.Configurations backend) { this.backend=backend; }
    void dock(float right,float top,float width,float viewportHeight) { this.right=right;this.top=top;this.width=width;this.viewportHeight=viewportHeight; }
    void collapse() { expanded=false;focused=false; }
    void blur() { focused=false; }
    void refresh() {
        run(()-> { backend.refresh();names=new ArrayList<>(backend.names());names.sort(String.CASE_INSENSITIVE_ORDER);if(!names.contains(selected)) selected=names.isEmpty()?null:names.get(0); });
    }
    private OpaiFeedback ink(String key) { return feedback.computeIfAbsent(key,k->new OpaiFeedback()); }
    private float listHeight() { return Math.min(144,Math.max(48,names.size()*24)); }
    private float maximumScroll() { return Math.max(0,names.size()*24-listHeight()); }
    private float targetHeight() { return page==1?76:page==2?66:listHeight()+50; }
    void update(double now) {
        popup.approach(expanded?1:0,now);body=heightMotion.approach(expanded?Math.min(targetHeight(),Math.max(48,viewportHeight-top-29)):0,now);
        pageFade.approach(1,now);scroll=clamp(scroll,0,maximumScroll());scrollMotion.approach(scroll,now);
        for(String name:names) selections.computeIfAbsent(name,n->new OpaiMotion(name.equals(selected)?1:0,25)).approach(name.equals(selected)?1:0,now);
    }
    boolean contains(float mx,float my) {
        if(!expanded && popup.value()<.005f) return hit(mx,my,right-chipWidth,top,chipWidth,11.75f);
        float factor=.82f+.18f*popup.value();float x=right+(mx-right)/factor,y=top+(my-top)/factor;
        return hit(x,y,right-width,top,width,21+body);
    }
    private float inputX(float x) { return right+(x-right)/(.82f+.18f*popup.value()); }
    private float inputY(float y) { return top+(y-top)/(.82f+.18f*popup.value()); }
    void paint(OpaiCanvas c,OpaiStyle.Palette p,float mouseX,float mouseY,double now) {
        chipWidth=Math.min(right,c.measure("Configurations",7.25f,"googlesans-regular",0)+3.75f);
        float opacity=popup.value();
        if(opacity<1) c.opacity((float)Math.pow(1-opacity,4),()-> {
            c.rounded(right-chipWidth,top,chipWidth,11.75f,3.875f,0xFF131316);
            ink("chip").paint(c,right-chipWidth,top,chipWidth,11.75f,3.875f,p.accent(),now);
            c.plainText("Configurations",right-chipWidth+2,top+5.875f,7.25f,chipWidth-3.75f,p.text());
        });
        if(opacity<=.005f) return;
        float mx=inputX(mouseX),my=inputY(mouseY);
        c.opacity(opacity,()->c.scale(.82f+.18f*opacity,right,top,()-> {
            float x=right-width;
            if(body>.01f) c.panel(x,top+21,width,body,false,p.body());
            c.panel(x,top,width,21,true,p.header());
            String title=page==1?"Add":page==2?"Settings":"Configurations";
            if(page!=0) icon(c,p,Icon.BACK,x+8,8,mx,my,now);
            c.text(title,x+10+(page==0?0:10),top+10.5f,9.2f,width-(page==0?48:30),p.text());
            if(page==0) { icon(c,p,Icon.PLUS,right-37,10,mx,my,now);icon(c,p,Icon.GEAR,right-21,10,mx,my,now); }
            c.clip(x,top+21,width,body,()-> {
                if(pageFade.value()<1) c.opacity(1-pageFade.value(),()->page(c,p,previousPage,mx,my,now));
                c.opacity(pageFade.value(),()->page(c,p,page,mx,my,now));
            });
            if(!message.isEmpty() && now<messageUntil) {
                c.rounded(x+3,top+24+body,width-6,15,3,p.field());
                c.text(message,x+7,top+31.5f+body,7,width-14,0xFFFF9999);
            }
        }));
    }
    private void page(OpaiCanvas c,OpaiStyle.Palette p,int page,float mx,float my,double now) {
        float x=right-width+8,y=top+24,w=width-16;
        if(page==0) {
            c.rounded(x,y,w,listHeight(),8,mix(p.field(),0xFF202020,.45f));
            c.clip(x,y,w,listHeight(),()-> {
                if(names.isEmpty()) c.text("No configurations",x+6,y+12,8.3f,w-12,p.fieldLine());
                for(int i=0;i<names.size();i++) {
                    String name=names.get(i);float rowY=y+i*24-scrollMotion.value();
                    if(rowY+24<=y || rowY>=y+listHeight()) continue;
                    float value=selections.get(name).value();
                    if(value>0) c.varying(x,rowY,w,24,i==0?8:0,i==0?8:0,i==names.size()-1?8:0,i==names.size()-1?8:0,alpha(p.accent(),value));
                    else if(hit(mx,my,x,rowY,w,24)) c.rect(x,rowY,w,24,p.hover());
                    ink("row:"+name).paint(c,x,rowY,w,24,0,p.accent(),now);
                    c.text(name+".json",x+6,rowY+12,8.3f,w-12,mix(p.text(),p.enabledText(),value));
                }
                if(maximumScroll()>0) {
                    float thumb=Math.max(12,(listHeight()-4)*listHeight()/(names.size()*24));
                    c.rounded(x+w-3,y+2+(listHeight()-4-thumb)*scrollMotion.value()/maximumScroll(),2,thumb,1,p.scrollbar());
                }
            });
            float buttonY=y+listHeight()+5,gap=6,cell=(w-gap*3)/4;
            Icon[] icons={Icon.REFRESH,Icon.SAVE,Icon.LOAD,Icon.DELETE};
            for(int i=0;i<4;i++) button(c,p,x+i*(cell+gap),buttonY,cell,"",icons[i],i==0||selected!=null,mx,my,now);
            float half=(w-gap)/2;
            button(c,p,x,buttonY+20,half,"Folder",Icon.FOLDER,true,mx,my,now);
            button(c,p,x+half+gap,buttonY+20,half,"Settings",Icon.GEAR,true,mx,my,now);
        } else if(page==1) {
            c.rounded(x,y,w,21,1.5f,focused?p.accent():p.toggleOutline());
            c.rounded(x+.8f,y+.8f,w-1.6f,19.4f,1,0xFF111014);
            if(focused) { c.rect(x+3,y-2,19,5,p.header());c.text("Name",x+4,y+.5f,5.5f,19,p.accent()); }
            c.clip(x+4,y+2,w-8,17,()-> {
                float textWidth=c.measure(input,8.3f),offset=Math.max(0,textWidth-w+14);
                c.text(input.isEmpty()&&!focused?"Name":input,x+6-offset,y+10.5f,8.3f,Math.max(w,textWidth+1),p.text());
                if(focused && ((long)now/500)%2==0) c.rect(x+6-offset+textWidth,y+4,.7f,13,p.text());
            });
            button(c,p,x,y+28,w,"Create",Icon.PLUS,true,mx,my,now);
            button(c,p,x,y+48,w,"Create Blank",Icon.PLUS,true,mx,my,now);
        } else {
            String[] labels={"Configurations","Visual settings","HUD editor"};
            for(int i=0;i<labels.length;i++) {
                if(hit(mx,my,x,y+i*19.6f,w,19.6f)) c.rect(x,y+i*19.6f,w,19.6f,p.hover());
                c.text(labels[i],x+2,y+19.6f*(i+.5f),8.3f,w-4,p.text());
            }
        }
    }
    private void icon(OpaiCanvas c,OpaiStyle.Palette p,Icon icon,float x,float size,float mx,float my,double now) {
        OpaiFeedback ink=ink("icon:"+icon);float hover=ink.hover(hit(mx,my,x-3,top+3,size+6,16),now);
        c.scale(1-.12f*ink.press(now),x+size/2,top+6+size/2,()->OpaiIcons.draw(c,icon,x,top+6,size,mix(p.fieldLine(),p.accent(),hover)));
    }
    private void button(OpaiCanvas c,OpaiStyle.Palette p,float x,float y,float w,String label,Icon icon,boolean active,float mx,float my,double now) {
        OpaiFeedback ink=ink(label.isEmpty()?icon.name():label);float hover=ink.hover(active&&hit(mx,my,x,y,w,18),now);
        c.scale(1-.06f*ink.press(now),x+w/2,y+9,()-> {
            c.rounded(x,y,w,18,9,mix(p.field(),p.accent(),.15f+.10f*hover));
            ink.paint(c,x,y,w,18,9,p.accent(),now);
            float textW=label.isEmpty()?0:c.measure(label,8.3f),left=x+(w-textW-9-(label.isEmpty()?0:5))/2;
            c.opacity(active?1:.4f,()-> { OpaiIcons.draw(c,icon,left,y+4.5f,9,p.text());if(!label.isEmpty()) c.text(label,left+14,y+9,8.3f,textW+1,p.text()); });
        });
    }
    void click(float mouseX,float mouseY,int button,double now) {
        if(button!=0) return;
        if(!expanded) { expanded=true;ink("chip").click(mouseX-right+chipWidth,mouseY-top,chipWidth,11.75f,now);return; }
        float mx=inputX(mouseX),my=inputY(mouseY),x=right-width;
        if(my<top+21) {
            if(page!=0 && mx<x+24) switchPage(0,now);
            else if(page==0 && mx>right-42 && mx<right-25) switchPage(1,now);
            else if(page==0 && mx>=right-25) switchPage(2,now);
            else expanded=false;
            return;
        }
        float left=x+8,y=top+24,w=width-16;
        if(page==0) {
            if(hit(mx,my,left,y,w,listHeight())) {
                int index=(int)((my-y+scrollMotion.value())/24);
                if(index<names.size()) { selected=names.get(index);ink("row:"+selected).click(mx-left,(my-y+scrollMotion.value())%24,w,24,now); }
            } else {
                float row=y+listHeight()+5,cell=(w-18)/4;
                if(hit(mx,my,left,row,w,18)) {
                    int index=(int)((mx-left)/(cell+6));
                    if(index==0) refresh();
                    else if(selected!=null) { final String name=selected;run(()-> { if(index==1) backend.save(name);else if(index==2) backend.load(name);else if(index==3) backend.delete(name); });refresh(); }
                    ink(new String[]{"REFRESH","SAVE","LOAD","DELETE"}[Math.min(index,3)]).click(0,9,cell,18,now);
                } else if(hit(mx,my,left,row+20,w,18)) {
                    if(mx<left+w/2) run(backend::folder);else switchPage(2,now);
                }
            }
        } else if(page==1) {
            focused=hit(mx,my,left,y,w,21);
            if(hit(mx,my,left,y+28,w,18)) create(false,now);
            else if(hit(mx,my,left,y+48,w,18)) create(true,now);
        } else {
            int index=(int)((my-y)/19.6f);
            if(index==0) switchPage(0,now);
            else if(index==1) { collapse();settings.run(); }
            else if(index==2) { collapse();edit.run(); }
        }
    }
    private void create(boolean blank,double now) {
        String name=input.trim();
        if(!name.matches("[A-Za-z0-9 _-]{1,14}")) { message="Use 1-14 letters or digits";messageUntil=now+3000;return; }
        if(names.contains(name)) { message="Name already exists";messageUntil=now+3000;return; }
        if(run(()->backend.create(name,blank))) { refresh();selected=name;input="";focused=false;switchPage(0,now); }
    }
    private void switchPage(int page,double now) { previousPage=this.page;this.page=page;pageFade.snap(0,now);pageFade.approach(1,now);focused=false; }
    boolean key(char character,int key) {
        double now=OpaiMotion.now();
        if(key==1 && expanded) { if(page!=0) switchPage(0,now);else collapse();return true; }
        if(!focused || page!=1) return false;
        if(key==28 || key==156) create(false,now);
        else if(key==14 && !input.isEmpty()) input=input.substring(0,input.offsetByCodePoints(input.length(),-1));
        else if(character>=32 && character!=127 && input.length()<14) input+=character;
        return true;
    }
    void wheel(int amount) { scroll=clamp(scroll-amount*48,0,maximumScroll()); }
    private boolean run(Runnable action) {
        try { action.run();return true; }
        catch(RuntimeException e) { message=e.getMessage()==null?"Operation failed":e.getMessage();messageUntil=OpaiMotion.now()+3000;return false; }
    }
}
