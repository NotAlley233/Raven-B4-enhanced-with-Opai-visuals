package xyz.daikirai.gui.opai;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

/** Real JNI + GPU smoke test, not an HTML mockup. It uses the production painters. */
public final class OpaiVisualCheck {
    private static final int PW=1920,PH=1080;
    private static void require(boolean pass,String message) { if(!pass) throw new AssertionError(message); }
    static final class Control implements OpaiUi.Control {
        final String name; final OpaiUi.Kind kind;final double min,max;double value;final String[] options;
        Control(String name,OpaiUi.Kind kind,double value,double min,double max,String... options) {this.name=name;this.kind=kind;this.value=value;this.min=min;this.max=max;this.options=options;}
        public String name(){return name;}public OpaiUi.Kind kind(){return kind;}public double value(){return value;}public double min(){return min;}public double max(){return max;}
        public String display(){return kind==OpaiUi.Kind.MODE?options[(int)value]:value==(int)value?Integer.toString((int)value):Double.toString(value);}
        public String[] options(){return options;}public void set(double value){this.value=OpaiMath.clamp(value,min,max);}
    }
    static final class Feature implements OpaiUi.Feature {
        final String name;boolean enabled;int binding;final List<OpaiUi.Control> controls=new ArrayList<>();
        Feature(String name,boolean enabled,OpaiUi.Control... controls){this.name=name;this.enabled=enabled;this.controls.addAll(Arrays.asList(controls));}
        public String name(){return name;}public boolean enabled(){return enabled;}public void toggle(){enabled=!enabled;}public boolean bindable(){return true;}public void bind(int key){binding=key;}public List<OpaiUi.Control> controls(){return controls;}
    }
    private static int loaded;
    private static OpaiUi fixture() {
        OpaiUi ui=new OpaiUi(new OpaiUi.Configurations(){
            public List<String> names(){return Arrays.asList("default","hypixel","practice");}public void refresh(){}public void load(String name){loaded++;}public void save(String name){}public void delete(String name){}public void create(String name,boolean blank){}public void folder(){}
        });
        String[][] names={{"AimAssist","AntiBot","AutoClicker","Backtrack","Criticals","Hitbox","KillAura","Reach","Velocity","WTap"},{"Fly","InvMove","LongJump","NoSlow","Speed","Sprint","Step"},{"AutoTool","ChestStealer","FastPlace","InvManager","NoFall","Scaffold"},{"BedESP","Chams","Fullbright","HUD","NameTags","TargetHUD","Tracers"},{"ChatCommands","Gui","IRC","Manager","Settings"}};
        for(int i=0;i<names.length;i++) for(int j=0;j<names[i].length;j++) ui.columns.get(i).features.add(new Feature(names[i][j],j%3==0));
        ui.columns.get(0).features.get(2).controls().addAll(Arrays.asList(new Control("Weapon only",OpaiUi.Kind.BOOLEAN,1,0,1),new Control("Min CPS",OpaiUi.Kind.NUMBER,10,1,20),new Control("Max CPS",OpaiUi.Kind.NUMBER,14,1,20),new Control("Mode",OpaiUi.Kind.MODE,0,0,2,"Normal","Jitter","Butterfly")));
        ui.pencil(true);return ui;
    }
    private static void image(File path) throws Exception {
        byte[] pixels=NativeNanoVG.pixels(PW,PH);BufferedImage image=new BufferedImage(PW,PH,BufferedImage.TYPE_INT_ARGB);
        int[] rgb=new int[PW*PH];int different=0;
        for(int y=0;y<PH;y++)for(int x=0;x<PW;x++) {
            int offset=((PH-1-y)*PW+x)*4;
            rgb[y*PW+x]=0xFF000000|(pixels[offset]&255)<<16|(pixels[offset+1]&255)<<8|(pixels[offset+2]&255);
            if((pixels[offset]&255)>80) different++;
        }
        require(different>5000,"Renderer produced an empty image");image.setRGB(0,0,PW,PH,rgb,0,PW);ImageIO.write(image,"png",path);
        System.out.println("RENDER "+path.getAbsolutePath()+" 1920x1080 visiblePixels="+different);
    }
    private static void frame(OpaiCanvas c,OpaiUi ui,OpaiStyle.Palette palette,double now,boolean notice) {
        NativeNanoVG.clearTestFrame();int[] before=NativeNanoVG.state();require(c.begin(960,540,PW,PH),"NanoVG unavailable");
        try {
            ui.paint(c,palette,-1,-1,now);
            DynamicIslandState island=new DynamicIslandState();OpaiIslandSurface surface=new OpaiIslandSurface(c);
            if(notice)island.post("module:Velocity","Module Toggled","Velocity has been ","Enabled",DynamicIslandState.Icon.TOGGLE,true,(long)now,800);
            DynamicIslandState.Frame frame=island.frame((long)now,960,new DynamicIslandStatus("Player","mc.hypixel.net",46,240),surface,true);
            DynamicIslandPainter.paint(surface,frame,960,palette);
        } finally {c.end();}
        require(Arrays.equals(before,NativeNanoVG.state()),"OpenGL state leaked into Minecraft");
        require(NativeNanoVG.glError()==0,"OpenGL error during NanoVG frame");
    }
    private static void hud(OpaiCanvas c,File out)throws Exception{
        DynamicIslandState island=new DynamicIslandState();OpaiIslandSurface surface=new OpaiIslandSurface(c);
        DynamicIslandStatus status=new DynamicIslandStatus("Player","mc.hypixel.net",46,240);
        DynamicIslandState.Frame idle=island.frame(0,960,status,surface,false);
        require(idle.rows().isEmpty() && idle.height()==23,"Island idle mismatch");
        island.post("module:Velocity","Module Toggled","Velocity has been ","Enabled",DynamicIslandState.Icon.TOGGLE,true,10,800);
        island.frame(10,960,status,surface,false);
        DynamicIslandState.Frame notice=island.frame(410,960,status,surface,false);
        require(notice.rows().size()==1 && notice.height()>30,"Island did not expand for notification");
        OpaiTargetPainter target=new OpaiTargetPainter(c);OpaiTargetPainter.Health health=new OpaiTargetPainter.Health();
        health.reset(20,20,0);health.update(8,20,400);OpaiTargetPainter.Health.Sample sample=health.sample(450);
        require(sample.health<20 && sample.health>8 && sample.trail>sample.health,"Damage trail response mismatch");
        NativeNanoVG.clearTestFrame();int[] before=NativeNanoVG.state();require(c.begin(960,540,PW,PH),"HUD renderer unavailable");
        try{
            DynamicIslandPainter.paint(surface,notice,960,OpaiStyle.LAVENDER);
            java.util.List<OpaiArraylistPainter.Row> rows=new java.util.ArrayList<>();int i=0;
            for(String name:new String[]{"AutoClicker","Velocity","Sprint"}){
                String suffix=i==0?"14 CPS":i==1?"Normal":"";
                float w=OpaiArraylistPainter.width(c,name,suffix);
                rows.add(new OpaiArraylistPainter.Row(name,suffix,959.5f-w,.25f+12*i++,w,OpaiArraylistPainter.measure(c,name),suffix.isEmpty()?0:OpaiArraylistPainter.measure(c," ")));
            }
            OpaiArraylistPainter.paint(c,OpaiStyle.LAVENDER,rows,true,true,true);
            target.paint(550,310,target.width("Opponent",20,960),"Opponent",sample,0,OpaiStyle.LAVENDER);
        }finally{c.end();}
        require(Arrays.equals(before,NativeNanoVG.state()),"Target texture uploads leaked GL state");image(new File(out,"opai-hud.png"));
        DynamicIslandState.Frame expired=island.frame(1200,960,status,surface,false);
        require(expired.rows().isEmpty(),"Island notice did not expire");
        island.postScaffold("64 Blocks",1,1201);DynamicIslandState.Frame scaffold=island.frame(1202,960,status,surface,true);
        require(scaffold.rows().size()==1 && scaffold.rows().get(0).icon()==DynamicIslandState.Icon.SCAFFOLD && scaffold.height()==50,"Scaffold status mismatch");
        NativeNanoVG.clearTestFrame();require(c.begin(960,540,PW,PH),"Scaffold frame unavailable");
        try{DynamicIslandPainter.paint(surface,scaffold,960,OpaiStyle.LIGHT_PINK);}finally{c.end();}
        image(new File(out,"opai-scaffold.png"));
        System.out.println("PASS island idle -> notice -> expiration -> scaffold; ArrayList geometry; target damage trail; texture upload GL state");
    }
    public static void main(String[] args) throws Exception {
        File out=new File(args[0]);require(out.isDirectory()||out.mkdirs(),"Cannot create verification directory");
        NativeNanoVG.load();NativeNanoVG.testContext(PW,PH);OpaiCanvas c=new OpaiCanvas();
        try {
            require(c.available(),"Cannot initialize renderer");for(int i=0;i<16 && NativeNanoVG.glError()!=0;i++){}
            require(NativeNanoVG.glError()==0,"Persistent GL initialization error");
            System.out.println("DRIVER "+NativeNanoVG.driver());
            OpaiUi ui=fixture();ui.open(960,540,0);frame(c,ui,OpaiStyle.LAVENDER,400,false);
            image(new File(out,"opai-lavender.png"));
            float x=40,y=540*.112f+21+19.6f*2+9;
            Feature feature=(Feature)ui.columns.get(0).features.get(2);boolean old=feature.enabled();
            ui.click(x,y,0,400);require(feature.enabled()!=old,"Module toggle did not reach model");
            ui.click(x,y,2,401);ui.key('g',34,402);require(feature.binding==34,"Middle-click key binding failed");
            ui.click(x,y,1,403);frame(c,ui,OpaiStyle.LAVENDER,404,true);frame(c,ui,OpaiStyle.LAVENDER,700,true);
            Control bool=(Control)feature.controls.get(0);ui.click(150,y+19.6f,0,701);require(bool.value==0,"Boolean did not change");
            Control number=(Control)feature.controls.get(1);ui.click(120,y+19.6f+19.6f+7,0,702);
            require(c.begin(960,540,PW,PH),"Slider frame unavailable");
            try{ui.paint(c,OpaiStyle.LAVENDER,150,y,800);}finally{c.end();}ui.release();require(number.value>10,"Slider drag did not change value");
            image(new File(out,"opai-expanded.png"));
            frame(c,ui,OpaiStyle.LIGHT_PINK,1000,false);image(new File(out,"opai-light-pink.png"));
            frame(c,ui,OpaiStyle.LAVENDER,1100,false);
            ui.click(880,59,0,1101);frame(c,ui,OpaiStyle.LAVENDER,1102,false);frame(c,ui,OpaiStyle.LAVENDER,1500,false);
            ui.click(870,168,0,1501);require(loaded==1,"Configuration popup load did not reach backend");image(new File(out,"opai-configurations.png"));
            hud(c,out);
            require(NativeNanoVG.glError()==0,"GL error after repeated frames");
            System.out.println("PASS module toggle, key binding, reveal, boolean, slider, palette, configuration popup, repeated frames, GL state restoration");
        } finally { c.close();NativeNanoVG.closeTestContext(); }
    }
}
