package xyz.daikirai.gui.opai;

import java.util.*;
import xyz.daikirai.client.ClientConfig;
import xyz.daikirai.module.GuiModule;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.util.Blur;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

/** Settings owns the opt-in visual mode; all four surfaces read the same saved palette. */
public final class OpaiRuntime {
    private static final Minecraft MC=Minecraft.getMinecraft();
    private static final OpaiCanvas CANVAS=new OpaiCanvas();
    private static final DynamicIslandState ISLAND=new DynamicIslandState();
    private static final OpaiIslandSurface ISLAND_SURFACE=new OpaiIslandSurface(CANVAS);
    public static float islandX,islandY,islandScale=1,arrayX,arrayY,arrayScale=1,targetScale=1;
    private static boolean previousEnabled;
    private static final Map<String,Box> BOXES=new LinkedHashMap<>();
    private OpaiRuntime(){}
    public static boolean enabled(){return SettingsModule.opaiVisuals!=null && SettingsModule.opaiVisuals.isToggled();}
    public static boolean ready(){return enabled() && CANVAS.available();}
    public static OpaiCanvas canvas(){return CANVAS;}
    public static OpaiStyle.Palette palette(){return SettingsModule.opaiColor!=null && SettingsModule.opaiColor.getInput()==1?OpaiStyle.LIGHT_PINK:OpaiStyle.LAVENDER;}
    public static long now(){return System.nanoTime()/1_000_000;}
    public static final class Box {
        public final String name;public final float x,y,width,height;
        Box(String name,float x,float y,float width,float height){this.name=name;this.x=x;this.y=y;this.width=width;this.height=height;}
        public boolean contains(float x,float y){return OpaiUi.hit(x,y,this.x,this.y,width,height);}
    }
    public static void drawn(String name,float x,float y,float width,float height){BOXES.put(name,new Box(name,x,y,width,height));}
    public static Collection<Box> boxes(){return new ArrayList<>(BOXES.values());}
    public static void resetLayout(){islandX=islandY=arrayX=arrayY=0;islandScale=arrayScale=targetScale=1;xyz.daikirai.module.render.TargetHudModule.offsetX=70;xyz.daikirai.module.render.TargetHudModule.offsetY=40;}
    public static void styleChanged(){ISLAND.clear();BOXES.clear();OpaiHud.reset();ClientConfig.save();}
    public static void saveLayout(java.io.FileWriter writer)throws java.io.IOException {
        writer.write("opai_island: "+islandX+","+islandY+","+islandScale+"\n");
        writer.write("opai_arraylist: "+arrayX+","+arrayY+","+arrayScale+"\n");
        writer.write("opai_target_scale: "+targetScale+"\n");
        writer.write("opai_target_position: "+xyz.daikirai.module.render.TargetHudModule.offsetX+","+xyz.daikirai.module.render.TargetHudModule.offsetY+"\n");
    }
    public static void loadLayout(String key,String value){
        try {
            String[] fields=value.split(",");
            if(key.equals("opai_island") && fields.length==3){islandX=finite(fields[0]);islandY=finite(fields[1]);islandScale=scale(fields[2]);}
            if(key.equals("opai_arraylist") && fields.length==3){arrayX=finite(fields[0]);arrayY=finite(fields[1]);arrayScale=scale(fields[2]);}
            if(key.equals("opai_target_scale"))targetScale=scale(value);
            if(key.equals("opai_target_position") && fields.length==2){xyz.daikirai.module.render.TargetHudModule.offsetX=(int)finite(fields[0]);xyz.daikirai.module.render.TargetHudModule.offsetY=(int)finite(fields[1]);}
        }catch(NumberFormatException ignored){}
    }
    private static float finite(String value){float result=Float.parseFloat(value);return Float.isFinite(result)?result:0;}
    private static float scale(String value){return OpaiMath.clamp(finite(value),.5f,2);}
    public static void menuBackground(){
        if(MC.theWorld==null)Gui.drawRect(0,0,new ScaledResolution(MC).getScaledWidth(),new ScaledResolution(MC).getScaledHeight(),0xFF191D26);
        if(GuiModule.backgroundBlur!=null && GuiModule.backgroundBlur.getInput()>0){
            Blur.get().startStencil();ScaledResolution sr=new ScaledResolution(MC);Gui.drawRect(0,0,sr.getScaledWidth(),sr.getScaledHeight(),-1);
            Blur.get().render(2,Math.max(1.7f,GuiModule.blurStrength));
        }
    }
    public static void blur(float x,float y,float width,float height,float radius){
        if(MC.theWorld==null || MC.currentScreen instanceof xyz.daikirai.gui.ClickGui || MC.currentScreen instanceof OpaiHudEditor || GuiModule.backgroundBlur==null || GuiModule.backgroundBlur.getInput()<=0) return;
        ScaledResolution sr=new ScaledResolution(MC);Blur.get().startStencil();
        if(CANVAS.begin(sr.getScaledWidth(),sr.getScaledHeight(),MC.displayWidth,MC.displayHeight)){
            try{CANVAS.rounded(x,y,width,height,radius,0xFFFFFFFF);}finally{CANVAS.end();}
        }
        Blur.get().render(2,.65f);
    }
    public static void moduleToggled(Module module){
        if(!enabled() || MC.thePlayer==null || module==ModuleManager.gui || module==ModuleManager.commandLine || module.noToggle)return;
        ISLAND.post("module:"+module.getName(),"Module Toggled",OpaiMath.display(module.getName())+" has been ",module.isEnabled()?"Enabled":"Disabled",DynamicIslandState.Icon.TOGGLE,module.isEnabled(),now(),800);
    }
    public static void success(String title,String detail){if(enabled())ISLAND.post(title,title,detail,"",DynamicIslandState.Icon.SUCCESS,true,now(),2600);}
    public static void renderIsland(){
        boolean current=enabled();
        if(previousEnabled!=current){ISLAND.clear();BOXES.clear();previousEnabled=current;}
        if(!current || MC.thePlayer==null || MC.theWorld==null || MC.gameSettings.showDebugInfo || MC.gameSettings.keyBindPlayerList.isKeyDown() || !ready())return;
        if(ModuleManager.enabled("Scaffold")!=null){
            int blocks=xyz.daikirai.module.player.AutoSwapModule.countBlocks();
            ISLAND.postScaffold(blocks+" Blocks",Math.min(1,blocks/64f),now());
        }else ISLAND.remove("scaffold");
        int ping=-1;
        if(MC.getNetHandler()!=null){net.minecraft.client.network.NetworkPlayerInfo info=MC.getNetHandler().getPlayerInfo(MC.thePlayer.getUniqueID());if(info!=null)ping=info.getResponseTime();}
        String server=MC.isSingleplayer()?null:MC.getCurrentServerData()==null?"multiplayer":MC.getCurrentServerData().serverIP;
        DynamicIslandStatus status=new DynamicIslandStatus(MC.thePlayer.getName(),server,ping,Minecraft.getDebugFPS());
        ScaledResolution sr=new ScaledResolution(MC);
        DynamicIslandState.Frame frame=ISLAND.frame(now(),sr.getScaledWidth()/islandScale,status,ISLAND_SURFACE,false);
        float x=(sr.getScaledWidth()-frame.width()*islandScale)/2+islandX,y=frame.top()*islandScale+islandY;
        x=OpaiMath.clamp(x,4,Math.max(4,sr.getScaledWidth()-frame.width()*islandScale-4));
        y=OpaiMath.clamp(y,4,Math.max(4,sr.getScaledHeight()-frame.height()*islandScale-4));
        drawn("Dynamic Island",x,y,frame.width()*islandScale,frame.height()*islandScale);
        blur(x,y,frame.width()*islandScale,frame.height()*islandScale,frame.radius()*islandScale);
        final float dx=x-(sr.getScaledWidth()/islandScale-frame.width())/2*islandScale,dy=y-frame.top()*islandScale;
        if(CANVAS.begin(sr.getScaledWidth(),sr.getScaledHeight(),MC.displayWidth,MC.displayHeight)){
            try{CANVAS.save();CANVAS.translate(dx,dy);CANVAS.scale(islandScale,islandScale);DynamicIslandPainter.paint(ISLAND_SURFACE,frame,sr.getScaledWidth()/islandScale,palette());}
            finally{CANVAS.restore();CANVAS.end();}
        }
    }
}
