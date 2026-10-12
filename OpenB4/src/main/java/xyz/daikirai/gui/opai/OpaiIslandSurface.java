// Island icon paths and shadow adapted from Samsara 23aa1b7 (GPL-3.0).
package xyz.daikirai.gui.opai;
public final class OpaiIslandSurface implements DynamicIslandPainter.Surface {
    private final OpaiCanvas c;
    public OpaiIslandSurface(OpaiCanvas c) { this.c=c; }
   @Override
   public void symbol(DynamicIslandStatus.Symbol symbol, float x, float y, float size, int argb) {
      c.save();
      try {
         c.translate(x, y);
         c.scale(size / 24, size / 24);
         int color=argb, strokeColor=argb; float strokeWidth=2.5f;
         c.path();
         switch (symbol) {
            case BED: {
               // A single filled frame avoids the seams of overlapping rounded rectangles.
               c.move(0, .75f);
               c.bezier(0, .336f, .336f, 0, .75f, 0);
               c.lineTo(2.25f, 0);
               c.bezier(2.664f, 0, 3, .336f, 3, .75f);
               c.lineTo(3, 9);
               c.lineTo(24, 9);
               c.lineTo(24, 15);
               c.lineTo(21, 15);
               c.lineTo(21, 12);
               c.lineTo(3, 12);
               c.lineTo(3, 15);
               c.lineTo(0, 15);
               c.closePath();
               c.circle(7.05f, 5.15f, 3.1f);
               c.move(10.8f, 3);
               c.lineTo(19.2f, 3);
               c.bezier(21.85f, 3, 24, 4.6f, 24, 6.6f);
               c.lineTo(24, 8.4f);
               c.lineTo(10.8f, 8.4f);
               c.closePath();
            
               break;
}
            case USER: {
               c.circle(12, 7.5f, 5);
               c.move(3.5f, 21);
               c.bezier(3.5f, 17, 6.5f, 14.5f, 10, 14.5f);
               c.lineTo(14, 14.5f);
               c.bezier(17.5f, 14.5f, 20.5f, 17, 20.5f, 21);
               c.quad(20.5f, 22, 19.5f, 22);
               c.lineTo(4.5f, 22);
               c.quad(3.5f, 22, 3.5f, 21);
               c.closePath();
            
               break;
}
            case LINK: {
               c.move(8, 16.5f);
               c.lineTo(6.5f, 16.5f);
               c.bezier(3.8f, 16.5f, 2.25f, 15, 2.25f, 12);
               c.bezier(2.25f, 9, 3.8f, 7.5f, 6.5f, 7.5f);
               c.lineTo(10, 7.5f);
               c.bezier(12.8f, 7.5f, 14, 9, 14, 12);
               c.lineTo(14, 13.25f);
               c.move(16, 7.5f);
               c.lineTo(17.5f, 7.5f);
               c.bezier(20.2f, 7.5f, 21.75f, 9, 21.75f, 12);
               c.bezier(21.75f, 15, 20.2f, 16.5f, 17.5f, 16.5f);
               c.lineTo(14, 16.5f);
               c.bezier(11.2f, 16.5f, 10, 15, 10, 12);
               c.lineTo(10, 10.75f);
               
               strokeWidth=2.5f;
               
               
               c.stroke(strokeWidth,strokeColor);
               return;
            }
            case REFRESH: {
               for (int i = 0; i < 2; i++) {
                  c.save();
                  if (i == 1) {
                     c.translate(24, 24);
                     c.rotate((float)Math.PI);
                  }
                  c.path();
                  c.move(12.5f, 1);
                  c.lineTo(4.5f, 6.5f);
                  c.lineTo(12.5f, 12);
                  c.quad(13, 12.25f, 13, 11.5f);
                  c.lineTo(13, 9);
                  c.bezier(17.5f, 9, 20.5f, 11.5f, 21.5f, 15.5f);
                  c.bezier(21.5f, 8.5f, 18.5f, 5.5f, 13, 5);
                  c.lineTo(13, 1.5f);
                  c.quad(13, .75f, 12.5f, 1);
                  c.closePath();
                  
                  c.fill(color);
                  c.restore();
               }
               return;
            }
            case CHROME: {
               // Keep the three lobes separate where parallel cuts meet the center clearance.
               float outerRadius = 12, innerRadius = 6, cutDistance = 5, gap = 1.75f;
               float upperCut = cutDistance + gap / 2, lowerCut = cutDistance - gap / 2;
               for (int i = 0; i < 3; i++) {
                  float rotation = (float)(i * Math.PI * 2 / 3);
                  float outerStart = rotation - (float)(Math.PI * 2 / 3) - (float)Math.asin(lowerCut / outerRadius);
                  float outerEnd = rotation - (float)Math.asin(upperCut / outerRadius);
                  float innerEnd = rotation - (float)Math.asin(upperCut / innerRadius);
                  float innerStart = rotation - (float)(Math.PI * 2 / 3) - (float)Math.asin(lowerCut / innerRadius);
                  c.path();
                  c.arc( 12, 12, outerRadius, outerStart, outerEnd, true);
                  c.lineTo(12 + innerRadius * (float)Math.cos(innerEnd), 12 + innerRadius * (float)Math.sin(innerEnd));
                  c.arc( 12, 12, innerRadius, innerEnd, innerStart, false);
                  c.closePath();
                  
                  c.fill(color);
               }
               c.path();
               c.circle(12, 12, 4);
            
               break;
}
         }
         
         c.fill(color);
      } finally {
         c.restore();
      }
   }


    private String font(float size,boolean breaking) { return breaking || size==8 ? "googlesans-bold" : "sourcesans3-semibold"; }
    private float tracking(float size) { return size==8 ? 0 : size==9 ? .125f : .14f; }
    public float measure(String text,float size) { return c.measure(text,size,font(size,false),tracking(size)); }
    public float measureBreaking(String text,float size) { return c.measure(text,size,font(size,true),tracking(size)); }
    public void text(String text,float x,float cy,float size,int color) { c.inkText(text,x,cy,size,font(size,false),tracking(size),color); }
    public void breakingText(String text,float x,float cy,float size,int color) { c.inkText(text,x,cy,size,font(size,true),tracking(size),color); }
    public void rounded(float x,float y,float w,float h,float r,int color) { c.rounded(x,y,w,h,r,color); }
    public void line(float x1,float y1,float x2,float y2,float stroke,int color) { c.line(x1,y1,x2,y2,stroke,color); }
    public void clip(float x,float y,float w,float h,Runnable content) { c.clip(x,y,w,h,content); }
    public void shadow(float x,float y,float w,float h,float radius) {
        for(float spread=8;spread>0;spread-=.25f) {
            int layer=DynamicIslandPainter.shadowLayer(spread); if(layer==0) continue;
            c.path(); c.roundedPath(x-spread,y-spread,w+2*spread,h+2*spread,radius+spread,radius+spread,radius+spread,radius+spread);
            c.roundedPath(x,y,w,h,radius,radius,radius,radius); c.hole(); c.fill(layer);
        }
    }
}
