from PIL import Image,ImageDraw,ImageFilter
import numpy as np,cv2
im=Image.open('public/assets/miyu.png').convert('RGBA')
a=np.array(im)
mask=np.zeros(a.shape[:2],dtype=np.uint8)
polys=[[(168,145),(175,134),(184,130),(193,131),(202,135),(209,142),(214,148),(208,155),(204,162),(191,165),(180,162),(173,156)],[(234,148),(239,140),(247,133),(257,130),(267,132),(274,138),(281,146),(277,153),(272,161),(261,165),(248,164),(240,158)]]
for p in polys: cv2.fillPoly(mask,[np.array(p)],255)
# Reconstruct face skin only in the eye sockets, not hair or eyebrows.
inpaint=a[:,:,:3].astype(float).copy()
soft=cv2.GaussianBlur(mask.astype(float)/255,(5,5),.55)
for y in range(126,169):
 for x in range(164,285):
  if soft[y,x]>0:
   t=np.clip((y-133)/32,0,1)
   skin=np.array([250,220,200])*(1-t)+np.array([247,211,194])*t
   inpaint[y,x]=inpaint[y,x]*(1-soft[y,x])+skin*soft[y,x]
blink=Image.fromarray(np.dstack([inpaint.astype(np.uint8),a[:,:,3]]))
s=4; big=blink.resize((blink.width*s,blink.height*s),Image.Resampling.LANCZOS);d=ImageDraw.Draw(big)
def curve(points,color,width):
 p=np.array(points,dtype=float); result=[]
 for t in np.linspace(0,1,50):
  q=(1-t)**3*p[0]+3*(1-t)**2*t*p[1]+3*(1-t)*t*t*p[2]+t**3*p[3]
  result.append(tuple(q*s))
 d.line(result,fill=color,width=int(width*s))
curve([(171,146),(184,152),(195,153),(207,145)],'#543b3f',2.1)
curve([(239,146),(251,154),(263,152),(275,145)],'#543b3f',2.1)
curve([(171,146),(171,146),(169,143),(168,142)],'#543b3f',1.3)
curve([(173,148),(172,148),(170,147),(169,145)],'#543b3f',1.1)
curve([(273,147),(276,146),(278,144),(279,142)],'#543b3f',1.3)
curve([(271,149),(274,148),(276,147),(277,146)],'#543b3f',1.0)
blink=big.resize(im.size,Image.Resampling.LANCZOS)
blink.save('public/assets/miyu-blink.webp',lossless=True)
# Subtle open-mouth speech phoneme for amplitude-driven mouth motion.
talk=im.resize((im.width*s,im.height*s),Image.Resampling.LANCZOS); d=ImageDraw.Draw(talk)
def ellipse(box,fill):d.ellipse(tuple(int(v*s) for v in box),fill=fill)
ellipse((212,178,235,190),'#f5d7c5')
ellipse((216,181,231,191),'#694047')
ellipse((219,186,229,191),'#d991a0')
d.line([(217*s,182*s),(228*s,182*s)],fill='#fff1df',width=2*s)
talk=talk.resize(im.size,Image.Resampling.LANCZOS);talk.save('public/assets/miyu-talk.webp',lossless=True)
panel=Image.new('RGB',(510,140),'#eee5df')
for i,frame in enumerate([im,blink,talk]):
 face=frame.crop((140,105,310,245));panel.paste(face,(i*170,0),face)
panel.resize((1020,280)).save('art/face-frames.jpg')
