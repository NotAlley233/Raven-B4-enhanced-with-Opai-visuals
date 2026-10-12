// Icon paths adapted from Samsara 23aa1b7 (GPL-3.0).
package xyz.daikirai.gui.opai;
final class OpaiIcons {
    enum Icon { PLUS, GEAR, BACK, REFRESH, SAVE, LOAD, DELETE, FOLDER, UPLOAD, CHECK }
    static void draw(OpaiCanvas c, Icon icon, float x, float y, float size, int color) {
        float strokeWidth=2.6f; int strokeColor=color;
        c.save(); try {
         c.translate(x, y);
         c.scale(size / 24, size / 24);
         
         
         strokeWidth=2.6f;
         
         
         c.path();
         switch (icon) {
            case CHECK: { c.move(2, 13); c.lineTo(9, 21); c.lineTo(23, 2); strokeWidth=1.3f; c.stroke(strokeWidth,strokeColor); 
               break;
}
            case PLUS: { line(c, 12, 2, 12, 22); line(c, 2, 12, 22, 12); c.stroke(strokeWidth,strokeColor); 
               break;
}
            case BACK: { c.move(15, 3); c.lineTo(6, 12); c.lineTo(15, 21); c.stroke(strokeWidth,strokeColor); 
               break;
}
            case GEAR: {
               for (int i = 0; i < 32; i++) {
                  double angle = i * Math.PI / 16;
                  float radius = (i % 4 == 0 || i % 4 == 3) ? 11 : 8.7f;
                  float px = 12 + (float)Math.cos(angle) * radius, py = 12 + (float)Math.sin(angle) * radius;
                  if (i == 0) c.move(px, py); else c.lineTo(px, py);
               }
               c.closePath();
               c.circle(12, 12, 4);
               c.hole();
               c.fill(color);
            
               break;
}
            case REFRESH: {
               c.arc( 12, 12, 8, -2.75f, -.35f, true); c.stroke(strokeWidth,strokeColor);
               c.path(); c.arc( 12, 12, 8, .39f, 2.79f, true); c.stroke(strokeWidth,strokeColor);
               c.path(); c.move(20, 10); c.lineTo(14.5f, 10); c.lineTo(20, 4.5f); c.closePath();
               c.move(4, 14); c.lineTo(9.5f, 14); c.lineTo(4, 19.5f); c.closePath(); c.fill(color);
            
               break;
}
            case SAVE: {
               c.move(3, 2); c.lineTo(18, 2); c.lineTo(22, 6);
               c.lineTo(22, 22); c.lineTo(3, 22); c.closePath();
               c.roundedPath( 7, 3, 9, 6, 0, 0, 0, 0); c.hole();
               c.roundedPath( 7, 13, 11, 7, 1, 1, 1, 1); c.hole(); c.fill(color);
            
               break;
}
            case LOAD: {
               c.roundedPath( 4, 3, 16, 19, 2, 2, 2, 2); c.fill(color);
               c.path(); c.roundedPath( 7, 1, 10, 4, 1, 1, 1, 1); c.fill(color);
               c.path(); line(c, 12, 8, 12, 17);
               c.move(8, 13); c.lineTo(12, 17); c.lineTo(16, 13);
               strokeWidth=1.8f;
               strokeColor=OpaiMath.alpha(0xFF262633,(color>>>24)/255f);  c.stroke(strokeWidth,strokeColor);
            
               break;
}
            case DELETE: {
               c.move(6, 7); c.lineTo(18, 7); c.lineTo(17, 22);
               c.lineTo(7, 22); c.closePath(); c.fill(color);
               c.path(); line(c, 4, 4, 20, 4); line(c, 9, 1, 15, 1); c.stroke(strokeWidth,strokeColor);
            
               break;
}
            case FOLDER: {
               c.move(2, 5); c.lineTo(10, 5); c.lineTo(12, 8);
               c.lineTo(22, 8); c.lineTo(22, 21); c.lineTo(2, 21);
               c.closePath(); c.fill(color);
            
               break;
}
            case UPLOAD: {
               line(c, 6, 12, 18, 5); line(c, 6, 12, 18, 19); c.stroke(strokeWidth,strokeColor);
               c.path(); c.circle(5, 12, 3.5f); c.circle(19, 4, 3.5f);
               c.circle(19, 20, 3.5f); c.fill(color);
            
               break;
}
         }

        } finally { c.restore(); }
    }
    private static void line(OpaiCanvas c,float x1,float y1,float x2,float y2) { c.move(x1,y1); c.lineTo(x2,y2); }
}
