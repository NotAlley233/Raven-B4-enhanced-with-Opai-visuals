/* Raven's Java 8 / LWJGL 2 bridge. NanoVG itself is unmodified and vendored. */
#include <windows.h>
#include <GL/glew.h>
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "nanovg.h"
#define NANOVG_GL2_IMPLEMENTATION
#include "nanovg_gl.h"

#define JNI_NAME(name) Java_xyz_daikirai_gui_opai_NativeNanoVG_##name
#define MAX_ATTRIBUTES 32
typedef struct {
    GLint enabled, size, type, normalized, stride, buffer;
    void *pointer;
} Attribute;
typedef struct {
    NVGcontext *vg;
    GLuint fbo, texture, depth;
    int width, height, frame;
    GLint framebuffer, renderbuffer, program, arrayBuffer, elementBuffer, activeTexture, matrixMode;
    GLint texture0, viewport[4], attributes;
    Attribute attrib[MAX_ATTRIBUTES];
} Renderer;
static HWND testWindow;
static HDC testDC;
static HGLRC testGL;

static void fail(JNIEnv *env, const char *message) {
    jclass cls = (*env)->FindClass(env, "java/lang/IllegalStateException");
    if (cls) (*env)->ThrowNew(env, cls, message);
}
static Renderer *renderer(jlong handle) { return (Renderer *)(intptr_t)handle; }
static NVGcolor ink(jint argb) {
    return nvgRGBA((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, ((uint32_t)argb >> 24) & 255);
}
static void saveState(Renderer *r) {
    glGetIntegerv(GL_FRAMEBUFFER_BINDING_EXT, &r->framebuffer);
    glGetIntegerv(GL_RENDERBUFFER_BINDING_EXT, &r->renderbuffer);
    glGetIntegerv(GL_CURRENT_PROGRAM, &r->program);
    glGetIntegerv(GL_ARRAY_BUFFER_BINDING, &r->arrayBuffer);
    glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, &r->elementBuffer);
    glGetIntegerv(GL_ACTIVE_TEXTURE, &r->activeTexture);
    glGetIntegerv(GL_MATRIX_MODE, &r->matrixMode);
    glGetIntegerv(GL_VIEWPORT, r->viewport);
    glGetIntegerv(GL_MAX_VERTEX_ATTRIBS, &r->attributes);
    if (r->attributes > MAX_ATTRIBUTES) r->attributes = MAX_ATTRIBUTES;
    for (int i = 0; i < r->attributes; ++i) {
        Attribute *a = &r->attrib[i];
        glGetVertexAttribiv(i, GL_VERTEX_ATTRIB_ARRAY_ENABLED, &a->enabled);
        glGetVertexAttribiv(i, GL_VERTEX_ATTRIB_ARRAY_SIZE, &a->size);
        glGetVertexAttribiv(i, GL_VERTEX_ATTRIB_ARRAY_TYPE, &a->type);
        glGetVertexAttribiv(i, GL_VERTEX_ATTRIB_ARRAY_NORMALIZED, &a->normalized);
        glGetVertexAttribiv(i, GL_VERTEX_ATTRIB_ARRAY_STRIDE, &a->stride);
        glGetVertexAttribiv(i, GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, &a->buffer);
        glGetVertexAttribPointerv(i, GL_VERTEX_ATTRIB_ARRAY_POINTER, &a->pointer);
    }
    glPushAttrib(GL_ALL_ATTRIB_BITS);
    glPushClientAttrib(GL_CLIENT_ALL_ATTRIB_BITS);
    glActiveTexture(GL_TEXTURE0);
    glGetIntegerv(GL_TEXTURE_BINDING_2D, &r->texture0);
    glMatrixMode(GL_PROJECTION); glPushMatrix();
    glMatrixMode(GL_MODELVIEW); glPushMatrix();
}
static void restoreState(Renderer *r) {
    glMatrixMode(GL_MODELVIEW); glPopMatrix();
    glMatrixMode(GL_PROJECTION); glPopMatrix();
    glPopClientAttrib();
    glPopAttrib();
    for (int i = 0; i < r->attributes; ++i) {
        Attribute *a = &r->attrib[i];
        glBindBuffer(GL_ARRAY_BUFFER, a->buffer);
        glVertexAttribPointer(i, a->size, a->type, a->normalized, a->stride, a->pointer);
        if (a->enabled) glEnableVertexAttribArray(i); else glDisableVertexAttribArray(i);
    }
    glBindBuffer(GL_ARRAY_BUFFER, r->arrayBuffer);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, r->elementBuffer);
    glUseProgram(r->program);
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, r->framebuffer);
    glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, r->renderbuffer);
    glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, r->texture0);
    glActiveTexture(r->activeTexture);
    glMatrixMode(r->matrixMode);
}
static int resize(Renderer *r, int width, int height) {
    if (r->width == width && r->height == height) return 1;
    if (!r->fbo) {
        glGenFramebuffersEXT(1, &r->fbo);
        glGenTextures(1, &r->texture);
        glGenRenderbuffersEXT(1, &r->depth);
    }
    glBindTexture(GL_TEXTURE_2D, r->texture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, r->fbo);
    glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, r->texture, 0);
    glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, r->depth);
    glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_DEPTH24_STENCIL8, width, height);
    glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT, r->depth);
    glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_STENCIL_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT, r->depth);
    if (glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT) != GL_FRAMEBUFFER_COMPLETE_EXT) return 0;
    r->width = width; r->height = height;
    return 1;
}
JNIEXPORT jlong JNICALL JNI_NAME(create)(JNIEnv *env, jclass cls) {
    if (!wglGetCurrentContext()) { fail(env, "NanoVG requires the Minecraft render thread and a current GL context"); return 0; }
    glewExperimental = GL_TRUE;
    if (glewInit() != GLEW_OK || !GLEW_VERSION_2_0 || !GLEW_EXT_framebuffer_object) {
        fail(env, "NanoVG needs OpenGL 2.0 and EXT_framebuffer_object"); return 0;
    }
    Renderer *r = calloc(1, sizeof(Renderer));
    if (!r) { fail(env, "Unable to allocate NanoVG renderer"); return 0; }
    saveState(r);
    r->vg = nvgCreateGL2(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
    restoreState(r);
    if (!r->vg) { free(r); fail(env, "Unable to create NanoVG GL2 context"); return 0; }
    return (jlong)(intptr_t)r;
}
JNIEXPORT void JNICALL JNI_NAME(destroy)(JNIEnv *env, jclass cls, jlong handle) {
    Renderer *r = renderer(handle); if (!r) return;
    if (r->frame) { nvgCancelFrame(r->vg); restoreState(r); }
    nvgDeleteGL2(r->vg);
    if (r->fbo) glDeleteFramebuffersEXT(1, &r->fbo);
    if (r->texture) glDeleteTextures(1, &r->texture);
    if (r->depth) glDeleteRenderbuffersEXT(1, &r->depth);
    free(r);
}
JNIEXPORT jint JNICALL JNI_NAME(font)(JNIEnv *env, jclass cls, jlong handle, jstring name, jbyteArray data) {
    Renderer *r = renderer(handle);
    jsize length = (*env)->GetArrayLength(env, data);
    unsigned char *bytes = malloc(length);
    if (!bytes) { fail(env, "Unable to allocate font data"); return -1; }
    (*env)->GetByteArrayRegion(env, data, 0, length, (jbyte *)bytes);
    const char *label = (*env)->GetStringUTFChars(env, name, NULL);
    saveState(r);
    int id = nvgCreateFontMem(r->vg, label, bytes, length, 1);
    restoreState(r);
    (*env)->ReleaseStringUTFChars(env, name, label);
    if (id < 0) fail(env, "Unable to load a bundled Opai font");
    return id;
}
JNIEXPORT void JNICALL JNI_NAME(begin)(JNIEnv *env, jclass cls, jlong handle, jfloat width, jfloat height, jint pw, jint ph) {
    Renderer *r = renderer(handle);
    if (r->frame) { fail(env, "Nested NanoVG frames are not allowed"); return; }
    saveState(r);
    if (pw <= 0 || ph <= 0 || !resize(r, pw, ph)) { restoreState(r); fail(env, "Unable to allocate NanoVG stencil framebuffer"); return; }
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, r->fbo);
    glViewport(0, 0, pw, ph);
    glDisable(GL_SCISSOR_TEST); glDisable(GL_DEPTH_TEST); glDisable(GL_ALPHA_TEST);
    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE); glStencilMask(0xFF);
    glClearColor(0, 0, 0, 0); glClearStencil(0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    nvgBeginFrame(r->vg, width, height, (float)pw / width);
    r->frame = 1;
}
JNIEXPORT void JNICALL JNI_NAME(end)(JNIEnv *env, jclass cls, jlong handle) {
    Renderer *r = renderer(handle); if (!r->frame) return;
    nvgEndFrame(r->vg);
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, r->framebuffer);
    glViewport(r->viewport[0], r->viewport[1], r->viewport[2], r->viewport[3]);
    glUseProgram(0); glDisable(GL_DEPTH_TEST); glDisable(GL_STENCIL_TEST);
    glDisable(GL_ALPHA_TEST); glDisable(GL_SCISSOR_TEST); glDisable(GL_CULL_FACE);
    glActiveTexture(GL_TEXTURE1); glDisable(GL_TEXTURE_2D);
    glActiveTexture(GL_TEXTURE0); glEnable(GL_TEXTURE_2D);
    glBindTexture(GL_TEXTURE_2D, r->texture);
    glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    glEnable(GL_BLEND); glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    glColor4f(1, 1, 1, 1);
    glMatrixMode(GL_PROJECTION); glLoadIdentity(); glOrtho(0, 1, 1, 0, -1, 1);
    glMatrixMode(GL_MODELVIEW); glLoadIdentity();
    glBegin(GL_QUADS);
    glTexCoord2f(0, 1); glVertex2f(0, 0); glTexCoord2f(1, 1); glVertex2f(1, 0);
    glTexCoord2f(1, 0); glVertex2f(1, 1); glTexCoord2f(0, 0); glVertex2f(0, 1);
    glEnd();
    r->frame = 0;
    restoreState(r);
}
JNIEXPORT void JNICALL JNI_NAME(command)(JNIEnv *env, jclass cls, jlong handle, jint op, jfloatArray args, jint color) {
    Renderer *r = renderer(handle); NVGcontext *vg = r->vg;
    float a[12] = {0}; jsize count = (*env)->GetArrayLength(env, args);
    if (count > 12) { fail(env, "Invalid NanoVG argument count"); return; }
    (*env)->GetFloatArrayRegion(env, args, 0, count, a);
    switch (op) {
    case 0: nvgSave(vg); break;
    case 1: nvgRestore(vg); break;
    case 2: nvgBeginPath(vg); nvgRoundedRect(vg,a[0],a[1],a[2],a[3],a[4]); nvgFillColor(vg,ink(color)); nvgFill(vg); break;
    case 3: nvgBeginPath(vg); nvgRoundedRectVarying(vg,a[0],a[1],a[2],a[3],a[4],a[5],a[6],a[7]); nvgFillColor(vg,ink(color)); nvgFill(vg); break;
    case 4: nvgIntersectScissor(vg,a[0],a[1],a[2],a[3]); break;
    case 5: nvgGlobalAlpha(vg,a[0]); break;
    case 6: nvgTranslate(vg,a[0],a[1]); break;
    case 7: nvgScale(vg,a[0],a[1]); break;
    case 8: nvgBeginPath(vg); break;
    case 9: nvgMoveTo(vg,a[0],a[1]); break;
    case 10: nvgLineTo(vg,a[0],a[1]); break;
    case 11: nvgBezierTo(vg,a[0],a[1],a[2],a[3],a[4],a[5]); break;
    case 12: nvgQuadTo(vg,a[0],a[1],a[2],a[3]); break;
    case 13: nvgCircle(vg,a[0],a[1],a[2]); break;
    case 14: nvgClosePath(vg); break;
    case 15: nvgFillColor(vg,ink(color)); nvgFill(vg); break;
    case 16: nvgStrokeColor(vg,ink(color)); nvgStrokeWidth(vg,a[0]); nvgLineCap(vg,NVG_ROUND); nvgLineJoin(vg,NVG_ROUND); nvgStroke(vg); break;
    case 17: nvgArc(vg,a[0],a[1],a[2],a[3],a[4],(int)a[5]); break;
    case 18: nvgPathWinding(vg,NVG_HOLE); break;
    case 19: nvgRotate(vg,a[0]); break;
    case 20: {
        NVGpaint p = nvgBoxGradient(vg,a[0],a[1]+1,a[2],a[3],a[4],6,ink(color),nvgRGBA(0,0,0,0));
        nvgBeginPath(vg); nvgRect(vg,a[0]-9,a[1]-8,a[2]+18,a[3]+18);
        nvgRoundedRect(vg,a[0],a[1],a[2],a[3],a[4]); nvgPathWinding(vg,NVG_HOLE);
        nvgFillPaint(vg,p); nvgFill(vg); break;
    }
    case 21: {
        NVGpaint p = nvgRadialGradient(vg,a[5],a[6],fmaxf(0,a[7]-.6f),a[7],ink(color),ink(color & 0xFFFFFF));
        nvgBeginPath(vg); nvgRoundedRect(vg,a[0],a[1],a[2],a[3],a[4]); nvgFillPaint(vg,p); nvgFill(vg); break;
    }
    case 22: nvgRoundedRectVarying(vg,a[0],a[1],a[2],a[3],a[4],a[5],a[6],a[7]); break;
    case 23: {
        NVGpaint p = nvgImagePattern(vg,a[0],a[1],a[2],a[3],0,(int)a[5],a[6]);
        nvgBeginPath(vg); nvgRoundedRect(vg,a[0],a[1],a[2],a[3],a[4]); nvgFillPaint(vg,p); nvgFill(vg); break;
    }
    }
}
JNIEXPORT jfloat JNICALL JNI_NAME(text)(JNIEnv *env, jclass cls, jlong handle, jbyteArray data, jfloat x, jfloat y, jfloat size, jint font, jint color, jfloat spacing, jboolean draw, jint alignment) {
    Renderer *r = renderer(handle); jsize length = (*env)->GetArrayLength(env,data);
    jbyte *bytes = (*env)->GetByteArrayElements(env,data,NULL);
    nvgFontFaceId(r->vg,font); nvgFontSize(r->vg,size); nvgTextLetterSpacing(r->vg,0);
    nvgFontBlur(r->vg,0); nvgTextAlign(r->vg,NVG_ALIGN_LEFT | (alignment & 127));
    float bounds[4];
    float width = nvgTextBounds(r->vg,0,0,(char *)bytes,(char *)bytes+length,bounds);
    int glyphCount=0;
    for(int i=0;i<length;i++) if(((unsigned char)bytes[i]&0xC0)!=0x80) glyphCount++;
    width+=glyphCount*spacing;
    if (draw) {
        nvgFillColor(r->vg,ink(color));
        if(alignment & 128) {
            nvgTextAlign(r->vg,NVG_ALIGN_LEFT | NVG_ALIGN_BASELINE);
            nvgTextBounds(r->vg,0,0,(char *)bytes,(char *)bytes+length,bounds);
            y-=(bounds[1]+bounds[3])/2;
        }
        if(spacing>0 && glyphCount>0) {
            NVGglyphPosition *glyphs=malloc(sizeof(NVGglyphPosition)*glyphCount);
            int count=nvgTextGlyphPositions(r->vg,0,0,(char *)bytes,(char *)bytes+length,glyphs,glyphCount);
            for(int i=0;i<count;i++) nvgText(r->vg,x+glyphs[i].x+i*spacing,y,glyphs[i].str,i+1<count?glyphs[i+1].str:(char *)bytes+length);
            free(glyphs);
        } else nvgText(r->vg,x,y,(char *)bytes,(char *)bytes+length);
    }
    (*env)->ReleaseByteArrayElements(env,data,bytes,JNI_ABORT);
    return width;
}
JNIEXPORT jint JNICALL JNI_NAME(image)(JNIEnv *env, jclass cls, jlong handle, jint width, jint height, jbyteArray data, jboolean nearest) {
    Renderer *r=renderer(handle);
    if ((*env)->GetArrayLength(env,data) != width*height*4) { fail(env,"Invalid RGBA image"); return 0; }
    jbyte *bytes=(*env)->GetByteArrayElements(env,data,NULL);
    Renderer saved={0}; saveState(&saved);
    int id=nvgCreateImageRGBA(r->vg,width,height,nearest?NVG_IMAGE_NEAREST:0,(unsigned char *)bytes);
    restoreState(&saved);
    (*env)->ReleaseByteArrayElements(env,data,bytes,JNI_ABORT); return id;
}
JNIEXPORT void JNICALL JNI_NAME(deleteImage)(JNIEnv *env, jclass cls, jlong handle, jint id) { Renderer saved={0}; saveState(&saved); nvgDeleteImage(renderer(handle)->vg,id); restoreState(&saved); }

/* A real, hidden WGL context for the renderer smoke test. No game or account is needed. */
JNIEXPORT void JNICALL JNI_NAME(testContext)(JNIEnv *env, jclass cls, jint width, jint height) {
    testWindow=CreateWindowExA(0,"STATIC","Raven NanoVG verification",WS_POPUP,0,0,width,height,NULL,NULL,GetModuleHandle(NULL),NULL);
    testDC=GetDC(testWindow);
    PIXELFORMATDESCRIPTOR p={sizeof(p),1,PFD_DRAW_TO_WINDOW|PFD_SUPPORT_OPENGL|PFD_DOUBLEBUFFER,PFD_TYPE_RGBA,32,0,0,0,0,0,0,8,0,0,0,0,0,24,8,0,PFD_MAIN_PLANE,0,0,0,0};
    int pf=ChoosePixelFormat(testDC,&p);
    if (!pf || !SetPixelFormat(testDC,pf,&p) || !(testGL=wglCreateContext(testDC)) || !wglMakeCurrent(testDC,testGL)) { fail(env,"Unable to create verification GL context"); return; }
    glViewport(0,0,width,height); glClearColor(.10f,.12f,.15f,1); glClear(GL_COLOR_BUFFER_BIT);
}
JNIEXPORT void JNICALL JNI_NAME(clearTestFrame)(JNIEnv *env,jclass cls) {
    glPushAttrib(GL_ALL_ATTRIB_BITS); glDisable(GL_SCISSOR_TEST); glClearColor(.10f,.12f,.15f,1); glColorMask(1,1,1,1); glClear(GL_COLOR_BUFFER_BIT); glPopAttrib();
}
JNIEXPORT jbyteArray JNICALL JNI_NAME(pixels)(JNIEnv *env, jclass cls, jint width, jint height) {
    size_t length=(size_t)width*height*4; unsigned char *bytes=malloc(length);
    glReadBuffer(GL_BACK); glPixelStorei(GL_PACK_ALIGNMENT,1); glFinish(); glReadPixels(0,0,width,height,GL_RGBA,GL_UNSIGNED_BYTE,bytes);
    jbyteArray result=(*env)->NewByteArray(env,(jsize)length);
    if (result) (*env)->SetByteArrayRegion(env,result,0,(jsize)length,(jbyte *)bytes);
    free(bytes); return result;
}
JNIEXPORT jint JNICALL JNI_NAME(glError)(JNIEnv *env, jclass cls) { return glGetError(); }
JNIEXPORT jstring JNICALL JNI_NAME(driver)(JNIEnv *env, jclass cls) {
    char text[512]; snprintf(text,sizeof(text),"%s | %s | %s",glGetString(GL_VENDOR),glGetString(GL_RENDERER),glGetString(GL_VERSION));
    return (*env)->NewStringUTF(env,text);
}
JNIEXPORT jintArray JNICALL JNI_NAME(state)(JNIEnv *env,jclass cls) {
    const GLenum keys[]={GL_FRAMEBUFFER_BINDING_EXT,GL_RENDERBUFFER_BINDING_EXT,GL_CURRENT_PROGRAM,GL_ARRAY_BUFFER_BINDING,GL_ELEMENT_ARRAY_BUFFER_BINDING,GL_ACTIVE_TEXTURE,GL_TEXTURE_BINDING_2D,GL_MATRIX_MODE,GL_BLEND_SRC,GL_BLEND_DST};
    GLint values[21]={0};
    for(int i=0;i<10;i++) glGetIntegerv(keys[i],values+i);
    glGetIntegerv(GL_VIEWPORT,values+10);
    values[14]=glIsEnabled(GL_BLEND); values[15]=glIsEnabled(GL_DEPTH_TEST); values[16]=glIsEnabled(GL_ALPHA_TEST);
    values[17]=glIsEnabled(GL_STENCIL_TEST); values[18]=glIsEnabled(GL_SCISSOR_TEST); values[19]=glIsEnabled(GL_CULL_FACE);values[20]=glIsEnabled(GL_TEXTURE_2D);
    jintArray result=(*env)->NewIntArray(env,21);(*env)->SetIntArrayRegion(env,result,0,21,values);return result;
}
JNIEXPORT void JNICALL JNI_NAME(closeTestContext)(JNIEnv *env, jclass cls) {
    wglMakeCurrent(NULL,NULL); if (testGL) wglDeleteContext(testGL); if (testDC) ReleaseDC(testWindow,testDC); if (testWindow) DestroyWindow(testWindow);
    testGL=NULL; testDC=NULL; testWindow=NULL;
}
