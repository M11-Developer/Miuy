// Lightweight GPU puppet: a deformable 2D mesh, not a Live2D or 3D model.
// All animation runs locally; no camera or facial data is used.
export class MiyuAvatar {
  constructor(canvas, options = {}) {
    this.canvas = canvas;
    this.options = { motion: .65, zoom: 1.88, animate: true, tracking: true, reduced: false, palette: 'rose', ...options };
    this.pointer = {x: 0, y: 0}; this.look = {x: 0, y: 0};
    this.action = ''; this.actionAt = 0; this.speaking = false; this.mouth = 0;
    this.blinkAt = performance.now() + 2200; this.blinkUntil = 0;
    this.loaded = false; this.lastFrame = 0; this.visible = true;
    this.gl = canvas.getContext('webgl', {alpha: true, antialias: true, premultipliedAlpha: false, preserveDrawingBuffer: true});
    if (!this.gl) this.ctx = canvas.getContext('2d');
    this.resizeObserver = new ResizeObserver(() => this.resize()); this.resizeObserver.observe(canvas);
    this.handlePointer = e => {
      if (!this.options.tracking) return;
      const r = canvas.getBoundingClientRect();
      this.pointer.x = Math.max(-1, Math.min(1, (e.clientX-r.left-r.width/2)/(r.width/2)));
      this.pointer.y = Math.max(-1, Math.min(1, (e.clientY-r.top-r.height*.25)/(r.height/2)));
    };
    window.addEventListener('pointermove', this.handlePointer, {passive:true});
    document.addEventListener('visibilitychange', () => {this.visible = !document.hidden;});
    this.ready = this.init();
  }
  async load(src) { return new Promise((resolve,reject) => {const img = new Image(); img.onload=()=>resolve(img);img.onerror=reject;img.src=src;}); }
  async init() {
    [this.image, this.blinkImage, this.talkImage] = await Promise.all([
      this.load('/assets/miyu.webp'),this.load('/assets/miyu-blink.webp'),this.load('/assets/miyu-talk.webp')
    ]);
    if (this.gl) {
      const gl = this.gl;
      const vertex = `
        attribute vec2 a_uv;
        varying vec2 v_uv;
        uniform vec2 u_resolution;
        uniform vec2 u_size;
        uniform vec2 u_origin;
        uniform float u_time;
        uniform float u_motion;
        uniform vec2 u_look;
        uniform float u_dance;
        uniform float u_pat;
        uniform float u_hello;
        uniform float u_hold;
        void main(){
          v_uv=a_uv;
          float x=a_uv.x;
          float y=a_uv.y;
          float weight=pow(1.0-y,1.6);
          float sway=sin(u_time*.63)*.009*u_motion;
          float breath=sin(u_time*1.4)*.0032*u_motion;
          float headWeight=1.0-smoothstep(.135,.255,y);
          float hairWeight=(1.0-smoothstep(.02,.14,abs(x-.5))) * 0.0;
          float sideWeight=smoothstep(.16,.34,abs(x-.5));
          float hairWave=sin(u_time*1.5-y*9.0)*.013*u_motion*sideWeight*(1.0-smoothstep(.40,.65,y))*smoothstep(.1,.23,y);
          float headTilt=(u_look.x*.006+sin(u_time*.44)*.002*u_motion+u_pat*.006*sin(u_time*7.0))*headWeight;
          vec2 p=vec2(x,y);
          p.x+=sway*weight+hairWave+u_look.x*.006*headWeight;
          p.y+=breath*(1.0-smoothstep(.6,.9,y))+(x-.5)*headTilt;
          p.x+=(x-.5)*breath*2.0*(1.0-smoothstep(.52,.7,y))*smoothstep(.14,.27,y);
          p.x+=sin(u_time*5.2)*.025*u_dance*weight;
          p.y-=abs(sin(u_time*5.2))*.007*u_dance;
          p.y+=sin(u_time*5.2)*(x-.5)*.016*u_dance;
          p.y+=u_look.y*.002*headWeight;
          // A small ear twitch; pinned at the base of each ear.
          float ear=(1.0-smoothstep(.03,.08,y))*smoothstep(.07,.20,abs(x-.5));
          p.x+=sin(u_time*9.0)*.004*u_motion*ear*pow(max(0.0,sin(u_time*.53)),16.0);
          // A gentle body greeting / nod, rather than a fake skeletal hand wave.
          p.y+=sin(u_time*4.0)*.005*u_hello*headWeight;
          // Holding a generated toy adds a soft, readable little bounce.
          p.x+=sin(u_time*3.2)*.010*u_hold*weight;
          p.y+=abs(sin(u_time*3.2))*.006*u_hold*weight;
          vec2 pixel=u_origin+p*u_size;
          vec2 clip=pixel/u_resolution*2.0-1.0;
          gl_Position=vec4(clip.x,-clip.y,0.0,1.0);
        }`;
      const fragment = `
        precision mediump float;
        varying vec2 v_uv;
        uniform sampler2D u_base;
        uniform sampler2D u_blinkTex;
        uniform sampler2D u_talkTex;
        uniform float u_blink;
        uniform float u_mouth;
        uniform float u_palette;
        uniform float u_blush;
        void main(){
          vec4 base=texture2D(u_base,v_uv);
          vec4 blink=texture2D(u_blinkTex,v_uv);
          vec4 talk=texture2D(u_talkTex,v_uv);
          float eyes=1.0-smoothstep(.027,.036,abs(v_uv.y-.121));
          vec4 col=mix(base,talk,u_mouth);
          col=mix(col,blink,u_blink*eyes);
          // Tint only rosy fabric/bows. Skin, eyes, white blouse, and black details remain unchanged.
          float pink=smoothstep(.018,.07,col.b-col.g)*smoothstep(.035,.10,col.r-col.g)*smoothstep(.33,.6,col.r);
          float notFace=1.0-(1.0-smoothstep(.0,.02,abs(v_uv.y-.126)-.055))*(1.0-smoothstep(.13,.21,abs(v_uv.x-.5)));
          pink*=notFace;
          vec3 lilac=vec3(col.r*.90,col.g*.98,min(1.0,col.b*1.12));
          vec3 peach=vec3(min(1.0,col.r*1.035),col.g*1.02,col.b*.90);
          if(u_palette>.5 && u_palette<1.5) col.rgb=mix(col.rgb,lilac,pink*.8);
          if(u_palette>1.5) col.rgb=mix(col.rgb,peach,pink*.85);
          float cheek1=1.0-smoothstep(0.0,.034,distance(v_uv*vec2(1.0,2.0),vec2(.383,.275)));
          float cheek2=1.0-smoothstep(0.0,.034,distance(v_uv*vec2(1.0,2.0),vec2(.618,.275)));
          col.rgb=mix(col.rgb,vec3(.94,.50,.56),max(cheek1,cheek2)*u_blush*.21);
          gl_FragColor=col;
        }`;
      const compile = (type,source) => {const s=gl.createShader(type);gl.shaderSource(s,source);gl.compileShader(s);if(!gl.getShaderParameter(s,gl.COMPILE_STATUS))throw new Error(gl.getShaderInfoLog(s));return s;};
      this.program=gl.createProgram();gl.attachShader(this.program,compile(gl.VERTEX_SHADER,vertex));gl.attachShader(this.program,compile(gl.FRAGMENT_SHADER,fragment));gl.linkProgram(this.program);
      if(!gl.getProgramParameter(this.program,gl.LINK_STATUS))throw new Error(gl.getProgramInfoLog(this.program));
      gl.useProgram(this.program);
      const verts=[];const nx=28,ny=64;
      for(let y=0;y<ny;y++) for(let x=0;x<nx;x++){
        const x0=x/nx,y0=y/ny,x1=(x+1)/nx,y1=(y+1)/ny;
        verts.push(x0,y0,x1,y0,x0,y1,x0,y1,x1,y0,x1,y1);
      }
      this.vertexCount=verts.length/2;
      const buffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buffer);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(verts),gl.STATIC_DRAW);
      const loc=gl.getAttribLocation(this.program,'a_uv');gl.enableVertexAttribArray(loc);gl.vertexAttribPointer(loc,2,gl.FLOAT,false,0,0);
      [this.image,this.blinkImage,this.talkImage].forEach((img,i)=>{
        const t=gl.createTexture();gl.activeTexture(gl.TEXTURE0+i);gl.bindTexture(gl.TEXTURE_2D,t);
        gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.LINEAR);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE);
        gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL,false);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,img);
      });
      this.uniforms={};['resolution','size','origin','time','motion','look','dance','pat','hello','hold','base','blinkTex','talkTex','blink','mouth','palette','blush'].forEach(n=>this.uniforms[n]=gl.getUniformLocation(this.program,'u_'+n));
      gl.uniform1i(this.uniforms.base,0);gl.uniform1i(this.uniforms.blinkTex,1);gl.uniform1i(this.uniforms.talkTex,2);
      gl.clearColor(0,0,0,0);
    }
    this.loaded=true;this.resize();this.frame(performance.now());
    return this;
  }
  resize(){
    const r=this.canvas.getBoundingClientRect();this.width=r.width||500;this.height=r.height||440;
    const dpr=Math.min(window.devicePixelRatio||1,2);this.canvas.width=Math.round(this.width*dpr);this.canvas.height=Math.round(this.height*dpr);
    if(this.gl)this.gl.viewport(0,0,this.canvas.width,this.canvas.height);
    if(this.ctx)this.ctx.setTransform(dpr,0,0,dpr,0,0);
  }
  configure(options){Object.assign(this.options,options);if(!this.options.tracking)this.pointer={x:0,y:0};}
  react(action){this.action=action;this.actionAt=performance.now();}
  setSpeaking(on){this.speaking=on;}
  frame(now){
    this.raf=requestAnimationFrame(t=>this.frame(t));
    if(!this.loaded||!this.visible||now-this.lastFrame<1000/45)return;
    this.lastFrame=now;
    const opts=this.options, motion=opts.animate&&!opts.reduced?opts.motion:0;
    const time=now/1000,elapsed=(now-this.actionAt)/1000;
    const act=this.action&&elapsed<5?Math.min(1,elapsed*3,5-elapsed):0;
    const isHappy=['pat','cheer','dance'].includes(this.action)&&act>0;
    if(now>this.blinkAt){this.blinkUntil=now+155;this.blinkAt=now+2800+Math.random()*4100;}
    const blink=opts.animate&&!opts.reduced&&(now<this.blinkUntil||this.action==='rest'&&act>.1||isHappy&&elapsed>.45&&elapsed<1.5)?1:0;
    this.look.x+=(this.pointer.x-this.look.x)*.035;this.look.y+=(this.pointer.y-this.look.y)*.035;
    const mouth=this.speaking?Math.max(0,Math.sin(now*.018)*.7+Math.sin(now*.036)*.25):0;
    this.mouth+=(mouth-this.mouth)*.45;
    const zoom=opts.zoom;
    const h=this.height*zoom,w=h*(this.image.width/this.image.height);
    const center=this.width<420?.54:.60;
    const x=this.width*center-w*.5,y=this.height*.035;
    this.bounds={x,y,w,h};
    if(this.gl){
      const gl=this.gl,u=this.uniforms;gl.clear(gl.COLOR_BUFFER_BIT);gl.useProgram(this.program);
      gl.uniform2f(u.resolution,this.width,this.height);gl.uniform2f(u.size,w,h);gl.uniform2f(u.origin,x,y);
      gl.uniform1f(u.time,time);gl.uniform1f(u.motion,motion);gl.uniform2f(u.look,opts.tracking&&!opts.reduced?this.look.x:0,opts.tracking&&!opts.reduced?this.look.y:0);
      gl.uniform1f(u.dance,this.action==='dance'&&motion?act:0);gl.uniform1f(u.pat,this.action==='pat'&&motion?act:0);gl.uniform1f(u.hello,this.action==='hello'&&motion?act:0);gl.uniform1f(u.hold,this.action==='hold'&&motion?act:0);
      gl.uniform1f(u.blink,blink);gl.uniform1f(u.mouth,opts.reduced?0:this.mouth);
      gl.uniform1f(u.palette,opts.palette==='lilac'?1:opts.palette==='peach'?2:0);gl.uniform1f(u.blush,isHappy?act:0);
      gl.drawArrays(gl.TRIANGLES,0,this.vertexCount);
    }else{
      const ctx=this.ctx;ctx.clearRect(0,0,this.width,this.height);ctx.save();
      ctx.translate(x+w/2,y+h);ctx.rotate(Math.sin(time*.7)*.006*motion);
      const frame=blink?this.blinkImage:this.mouth>.2?this.talkImage:this.image;
      if(opts.palette==='lilac')ctx.filter='hue-rotate(16deg)';if(opts.palette==='peach')ctx.filter='hue-rotate(-10deg)';
      ctx.drawImage(frame,-w/2,-h,w,h);ctx.restore();
    }
  }
  async portrait(scene){
    const out=document.createElement('canvas');out.width=1400;out.height=1050;const c=out.getContext('2d');
    if(scene==='studio'){const g=c.createLinearGradient(0,0,1400,1050);g.addColorStop(0,'#fff5eb');g.addColorStop(1,'#d5c4d5');c.fillStyle=g;c.fillRect(0,0,1400,1050);}
    else {const img=await this.load(scene==='garden'?'/assets/garden.webp':'/assets/room.webp');const scale=Math.max(1400/img.width,1050/img.height);c.drawImage(img,(1400-img.width*scale)/2,(1050-img.height*scale)/2,img.width*scale,img.height*scale);}
    const ch=1210,cw=ch*this.image.width/this.image.height;c.drawImage(this.image,710-cw/2,40,cw,ch);
    const g=c.createLinearGradient(0,650,0,1050);g.addColorStop(0,'#f4e9df00');g.addColorStop(1,scene==='garden'?'#272d51dd':'#efe2d5ee');c.fillStyle=g;c.fillRect(0,650,1400,400);
    c.fillStyle=scene==='garden'?'#fff1ea':'#625447';c.font='16px Manrope';c.fillText('YOUR EVERYDAY COMPANION',72,888);c.font='54px Lora, Georgia';c.fillText('Miyu Hoshino',70,960);c.font='16px Manrope';c.fillText('A little company. A little magic.',74,997);
    return new Promise(resolve=>out.toBlob(resolve,'image/png'));
  }
}
