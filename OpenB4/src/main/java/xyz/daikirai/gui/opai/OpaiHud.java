package xyz.daikirai.gui.opai;

import java.util.*;
import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.HudModule;
import xyz.daikirai.module.render.TargetHudModule;

public final class OpaiHud {
    private static final Minecraft MC=Minecraft.getMinecraft();
    private static final Map<Module,Motion> MOTIONS=new IdentityHashMap<>();
    private static final OpaiTargetPainter TARGET=new OpaiTargetPainter(OpaiRuntime.canvas());
    private static final OpaiTargetPainter.Health HEALTH=new OpaiTargetPainter.Health();
    private static String targetName;
    private static EntityLivingBase targetEntity;
    private static ResourceLocation skin;
    private static int face;
    private OpaiHud(){}
    static void reset(){MOTIONS.clear();targetName=null;targetEntity=null;skin=null;if(face!=0 && OpaiRuntime.canvas().available())OpaiRuntime.canvas().deleteImage(face);face=0;}
    private static final class Motion {
        final OpaiSpring horizontal=new OpaiSpring(0,25),occupied=new OpaiSpring(0,12.7,true);
        boolean enabled;long changed=Long.MIN_VALUE/2;
        void update(boolean enabled,long now){if(this.enabled!=enabled){this.enabled=enabled;changed=now;}horizontal.to(enabled?1:0,now);occupied.to(enabled || now-changed<65?1:0,now);}
    }
    private static final class Item {
        Module module;String name,suffix;float width,nameWidth,gap;Motion motion;
    }
    public static boolean arraylist(){
        if(!OpaiRuntime.ready())return false;
        ScaledResolution sr=new ScaledResolution(MC);OpaiCanvas c=OpaiRuntime.canvas();
        List<Item> items=new ArrayList<>();long now=OpaiRuntime.now();
        for(Module module:HudModule.listed()){
            Motion motion=MOTIONS.computeIfAbsent(module,m->new Motion());motion.update(module.isEnabled(),now);
            if(!module.isEnabled() && motion.horizontal.value()<=.001 && motion.occupied.value()<=.001)continue;
            Item item=new Item();item.module=module;item.motion=motion;item.name=OpaiMath.display(module.getName());
            if(HudModule.lowercase.isToggled())item.name=item.name.toLowerCase(Locale.ROOT);
            String info=HudModule.showModuleInfo.isToggled()?module.getInfo():"";
            item.suffix=info==null?"":info.replaceAll("§.","");
            if(HudModule.lowercase.isToggled())item.suffix=item.suffix.toLowerCase(Locale.ROOT);
            item.nameWidth=OpaiArraylistPainter.measure(c,item.name);item.gap=item.suffix.isEmpty()?0:OpaiArraylistPainter.measure(c," ");
            item.width=6+item.nameWidth+item.gap+OpaiArraylistPainter.measure(c,item.suffix);items.add(item);
        }
        items.sort((a,b)->Float.compare(b.width,a.width));
        List<OpaiArraylistPainter.Row> rows=new ArrayList<>();float y=.25f,max=0;
        float logicalWidth=sr.getScaledWidth()/OpaiRuntime.arrayScale;
        for(Item item:items){
            float slide=(1-(float)OpaiMath.clamp(item.motion.horizontal.value(),0,1))*(item.width+4),x=logicalWidth-.5f-item.width+slide;
            rows.add(new OpaiArraylistPainter.Row(item.name,item.suffix,x,y,item.width,item.nameWidth,item.gap));
            max=Math.max(max,item.width);y+=12*(float)OpaiMath.clamp(item.motion.occupied.value(),0,1);
        }
        OpaiRuntime.drawn("ArrayList",sr.getScaledWidth()-max*OpaiRuntime.arrayScale-.5f+OpaiRuntime.arrayX,.25f+OpaiRuntime.arrayY,max*OpaiRuntime.arrayScale,y*OpaiRuntime.arrayScale);
        if(c.begin(sr.getScaledWidth(),sr.getScaledHeight(),MC.displayWidth,MC.displayHeight)){
            try{c.save();c.translate(OpaiRuntime.arrayX,OpaiRuntime.arrayY);c.scale(OpaiRuntime.arrayScale,OpaiRuntime.arrayScale);OpaiArraylistPainter.paint(c,OpaiRuntime.palette(),rows,HudModule.drawBackground.isToggled(),true,true);}
            finally{c.restore();c.end();}
        }
        return true;
    }
    public static boolean target(xyz.daikirai.util.Animation fade,String name,float health,double ratio){
        if(!OpaiRuntime.ready())return false;
        EntityLivingBase entity=null;
        if(xyz.daikirai.module.combat.KillAuraModule.target!=null && xyz.daikirai.module.combat.KillAuraModule.target.entity instanceof EntityLivingBase)entity=(EntityLivingBase)xyz.daikirai.module.combat.KillAuraModule.target.entity;
        if(entity==null && MC.theWorld!=null)entity=MC.theWorld.getPlayerEntityByName(name);
        if(MC.currentScreen instanceof OpaiHudEditor)entity=MC.thePlayer;
        if(!name.equals(targetName))targetEntity=null;
        if(entity!=null)targetEntity=entity;
        long now=OpaiRuntime.now();float maximum=entity!=null?entity.getMaxHealth():ratio>0?(float)(health/ratio):20;
        if(!name.equals(targetName)){targetName=name;HEALTH.reset(health,maximum,now);}
        OpaiTargetPainter.Health.Sample sample=HEALTH.update(health,maximum,now);
        ScaledResolution sr=new ScaledResolution(MC);OpaiCanvas c=OpaiRuntime.canvas();float scale=OpaiRuntime.targetScale;
        float w=TARGET.width(name,maximum,sr.getScaledWidth()/scale),x=sr.getScaledWidth()/2f+TargetHudModule.offsetX,y=sr.getScaledHeight()/2f+TargetHudModule.offsetY;
        x=OpaiMath.clamp(x,2,Math.max(2,sr.getScaledWidth()-w*scale-2));y=OpaiMath.clamp(y,2,Math.max(2,sr.getScaledHeight()-40*scale-2));
        final float px=x,py=y,opacity=fade==null?1:Math.max(0,1-fade.getValueInt(0,255,1)/255f);
        if(opacity<=.01f)return true;
        OpaiRuntime.drawn("Target HUD",x,y,w*scale,40*scale);OpaiRuntime.blur(x,y,w*scale,40*scale,6*scale);
        updateFace(c,targetEntity);
        if(c.begin(sr.getScaledWidth(),sr.getScaledHeight(),MC.displayWidth,MC.displayHeight)){
            try{c.opacity(opacity,()->{c.save();try{c.translate(px,py);c.scale(scale,scale);TARGET.paint(0,0,w,name,sample,face,OpaiRuntime.palette());}finally{c.restore();}});}
            finally{c.end();}
        }
        if(targetEntity!=null && opacity>.1f){
            GlStateManager.pushMatrix();
            try{
                GlStateManager.translate(x,y,0);GlStateManager.scale(scale,scale,1);
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                for(int i=0;i<4;i++){
                    ItemStack stack=targetEntity.getCurrentArmor(3-i);if(stack==null)continue;
                    GlStateManager.pushMatrix();GlStateManager.translate(31.5f+i*15.5f,14.5f,0);GlStateManager.scale(.95f,.95f,1);
                    MC.getRenderItem().renderItemAndEffectIntoGUI(stack,0,0);GlStateManager.popMatrix();
                }
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            }finally{GlStateManager.popMatrix();GlStateManager.color(1,1,1,1);}
        }
        return true;
    }
    private static void updateFace(OpaiCanvas c,EntityLivingBase entity){
        ResourceLocation next=entity instanceof AbstractClientPlayer?((AbstractClientPlayer)entity).getLocationSkin():DefaultPlayerSkin.getDefaultSkinLegacy();
        if(next.equals(skin) && face!=0)return;
        int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        MC.getTextureManager().bindTexture(next);
        try{
            int width=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH),height=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
            if(width<64 || height<32)return;
            ByteBuffer pixels=BufferUtils.createByteBuffer(width*height*4);GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
            BufferedImage portrait=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);int unit=width/64;
            for(int y=0;y<8;y++)for(int x=0;x<8;x++){
                int base=((y+8)*unit*width+(x+8)*unit)*4,hat=((y+8)*unit*width+(x+40)*unit)*4;
                float a=(pixels.get(hat+3)&255)/255f;
                int red=Math.round((pixels.get(base)&255)*(1-a)+(pixels.get(hat)&255)*a),green=Math.round((pixels.get(base+1)&255)*(1-a)+(pixels.get(hat+1)&255)*a),blue=Math.round((pixels.get(base+2)&255)*(1-a)+(pixels.get(hat+2)&255)*a);
                portrait.setRGB(x,y,0xFF000000|red<<16|green<<8|blue);
            }
            if(face!=0)c.deleteImage(face);face=c.image(8,8,OpaiTargetPainter.rgba(portrait));skin=next;
        }finally{GlStateManager.bindTexture(previous);}
    }
}
