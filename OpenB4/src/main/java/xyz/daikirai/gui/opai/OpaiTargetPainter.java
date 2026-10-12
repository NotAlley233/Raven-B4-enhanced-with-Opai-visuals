package xyz.daikirai.gui.opai;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

/** Uses Samsara's original 4x shell texture; only the neutral middle section stretches. */
public final class OpaiTargetPainter {
    private final OpaiCanvas c;
    private int left,middle,right;
    public OpaiTargetPainter(OpaiCanvas c){this.c=c;}
    public static byte[] rgba(BufferedImage image){
        byte[] rgba=new byte[image.getWidth()*image.getHeight()*4];
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int color=image.getRGB(x,y),offset=(y*image.getWidth()+x)*4;
            rgba[offset]=(byte)(color>>16);rgba[offset+1]=(byte)(color>>8);rgba[offset+2]=(byte)color;rgba[offset+3]=(byte)(color>>>24);
        }
        return rgba;
    }
    private void textures(){
        if(left!=0)return;
        try{
            BufferedImage image=ImageIO.read(new ByteArrayInputStream(NativeNanoVG.resource("textures/target-opai.png")));
            left=upload(image.getSubimage(0,0,56,224));middle=upload(image.getSubimage(56,0,440,224));right=upload(image.getSubimage(496,0,56,224));
        }catch(Exception error){throw new IllegalStateException("Cannot load Opai target panel",error);}
    }
    private int upload(BufferedImage image){return c.image(image.getWidth(),image.getHeight(),rgba(image),false);}
    public float measure(String text){return c.measure(text,8,"googlesans-medium",0);}
    public float width(String name,float maximum,float available){return Math.min(Math.max(122,(float)Math.ceil(35+measure(name)+labelWidth(maximum)+4)),Math.max(1,available-4));}
    private float labelWidth(float maximum){String label=Health.format(maximum);return Math.max(measure(label.contains(".")?label:label+".0"),measure("00.0"));}
    public void paint(float x,float y,float width,String name,Health.Sample health,int face,OpaiStyle.Palette p){
        textures();
        c.image(left,x-8,y-8,14,56,0,1);c.image(middle,x+6,y-8,Math.max(0,width-12),56,0,1);c.image(right,x+width-6,y-8,14,56,0,1);
        if(face!=0)c.image(face,x+3,y+3,26,26,4.5f,1);
        else{c.rounded(x+3,y+3,26,26,4.5f,0xFF2D2D38);OpaiIcons.draw(c,OpaiIcons.Icon.GEAR,x+9,y+9,14,p.accent());}
        String label=health.label(),fitted=fit(name,width-35-labelWidth(health.maximum)-3);
        c.baselineText(fitted,x+32,y+12.7f,8,"googlesans-medium",0xFFFFFFFF);
        c.baselineText(label,x+32+measure(fitted)+1.5f,y+12.7f,8,"googlesans-medium",p.accent());
        float bar=width-7;
        c.rounded(x+3,y+32,bar,5,2.5f,0x66000000);
        c.rounded(x+3,y+31.25f,bar*health.trailFraction(),4.5f,2.25f,0x66FFFFFF);
        c.rounded(x+3,y+31.25f,bar*health.fraction(),4.5f,2.25f,p.accent());
    }
    private String fit(String text,float maximum){
        if(measure(text)<=maximum)return text;
        int end=text.length();while(end>0 && measure(text.substring(0,end)+"…")>maximum)end=text.offsetByCodePoints(end,-1);
        return end==0?"":text.substring(0,end)+"…";
    }
    /** Exact fast health response plus a 100ms delayed damage trail from Samsara. */
    public static final class Health {
        private float goal,maximum=20,fillFrom,trailFrom;private long changedAt;private boolean damage,initialized;
        public static final class Sample {
            public final float health,trail,maximum;
            Sample(float health,float trail,float maximum){this.health=health;this.trail=trail;this.maximum=maximum;}
            public float fraction(){return health/maximum;}public float trailFraction(){return trail/maximum;}public String label(){return format(health);}
        }
        public void reset(float health,float maximum,long now){this.maximum=maximum(maximum);this.goal=health(health,this.maximum);fillFrom=trailFrom=goal;changedAt=now;damage=false;initialized=true;}
        public Sample update(float health,float maximum,long now){
            float max=maximum(maximum),value=health(health,max);if(!initialized)reset(value,max,now);
            if(value!=goal || max!=this.maximum){Sample current=sample(now);damage=value<current.health;fillFrom=current.health;trailFrom=Math.max(current.health,current.trail);goal=value;this.maximum=max;changedAt=now;}
            return sample(now);
        }
        public Sample sample(long now){double elapsed=Math.max(0,now-changedAt);float fill=response(fillFrom,goal,elapsed,65),trail=damage?Math.max(fill,response(trailFrom,goal,Math.max(0,elapsed-100),220)):fill;return new Sample(health(fill,maximum),health(trail,maximum),maximum);}
        private static float response(float from,float to,double elapsed,double decay){float value=(float)(to+(from-to)*Math.exp(-elapsed/decay));return Math.abs(value-to)<.025f?to:value;}
        private static float maximum(float value){return Float.isFinite(value)?Math.max(1,value):20;}
        private static float health(float value,float maximum){return Float.isFinite(value)?OpaiMath.clamp(value,0,maximum):0;}
        public static String format(float health){float value=Math.round(health*10)/10f;return value==Math.round(value)?Integer.toString(Math.round(value)):String.format(java.util.Locale.ROOT,"%.1f",value);}
    }
}
