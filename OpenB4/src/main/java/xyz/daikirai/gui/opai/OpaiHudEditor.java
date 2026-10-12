package xyz.daikirai.gui.opai;

import java.io.IOException;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import xyz.daikirai.client.ClientConfig;
import xyz.daikirai.module.render.TargetHudModule;

/** Shared drag/scale editor for the complete Opai HUD. */
public final class OpaiHudEditor extends xyz.daikirai.gui.HudEditor {
    private String selected;
    private int lastX,lastY;
    @Override public boolean doesGuiPauseGame(){return false;}
    @Override public void drawScreen(int x,int y,float partial){
        OpaiRuntime.menuBackground();
        if(mc.thePlayer!=null){OpaiHud.arraylist();OpaiHud.target(null,mc.thePlayer.getName(),mc.thePlayer.getHealth(),mc.thePlayer.getHealth()/mc.thePlayer.getMaxHealth());}
        OpaiRuntime.renderIsland();
        for(OpaiRuntime.Box box:OpaiRuntime.boxes())drawRect((int)box.x-1,(int)box.y-1,(int)(box.x+box.width)+1,(int)box.y,OpaiRuntime.palette().accent());
        drawCenteredString(fontRendererObj,"Drag HUD | Scroll to scale | R to reset | Esc to save",width/2,height-25,0xFFFFFF);
    }
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{
        if(button==0)for(OpaiRuntime.Box box:OpaiRuntime.boxes())if(box.contains(x,y)){selected=box.name;lastX=x;lastY=y;break;}
    }
    @Override protected void mouseClickMove(int x,int y,int button,long elapsed){
        if(button!=0 || selected==null)return;
        int dx=x-lastX,dy=y-lastY;lastX=x;lastY=y;
        if(selected.equals("Dynamic Island")){OpaiRuntime.islandX+=dx;OpaiRuntime.islandY+=dy;}
        else if(selected.equals("ArrayList")){OpaiRuntime.arrayX+=dx;OpaiRuntime.arrayY+=dy;}
        else{TargetHudModule.offsetX+=dx;TargetHudModule.offsetY+=dy;}
    }
    @Override protected void mouseReleased(int x,int y,int button){selected=null;}
    @Override public void handleMouseInput()throws IOException{
        super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel==0)return;
        int x=Mouse.getEventX()*width/mc.displayWidth,y=height-Mouse.getEventY()*height/mc.displayHeight-1;
        for(OpaiRuntime.Box box:OpaiRuntime.boxes())if(box.contains(x,y)){
            float delta=wheel>0?.05f:-.05f;
            if(box.name.equals("Dynamic Island"))OpaiRuntime.islandScale=OpaiMath.clamp(OpaiRuntime.islandScale+delta,.5f,2);
            else if(box.name.equals("ArrayList"))OpaiRuntime.arrayScale=OpaiMath.clamp(OpaiRuntime.arrayScale+delta,.5f,2);
            else OpaiRuntime.targetScale=OpaiMath.clamp(OpaiRuntime.targetScale+delta,.5f,2);
            break;
        }
    }
    @Override protected void keyTyped(char character,int key){if(key==19)OpaiRuntime.resetLayout();else super.keyTyped(character,key);}
    @Override public void onGuiClosed(){ClientConfig.save();}
}
