"""Render an original 30-second motion-graphics film, with optional local narration."""
from pathlib import Path
import math, subprocess, wave
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import imageio_ffmpeg

ROOT = Path(__file__).resolve().parents[1]
TEMP = ROOT/'.runtime/promo'
OUT = ROOT/'02-frontend-web/public/media'
W,H,FPS,DURATION = 1280,720,24,30
INK='#37352f'; MUTED='#958b7c'; GOLD='#b77945'; LINE='#d9d2c5'; SAGE='#66816b'
FONT='C:/Windows/Fonts/msyh.ttc'; BOLD='C:/Windows/Fonts/msyhbd.ttc'
fonts={}
def font(size,bold=False):
    key=(size,bold)
    if key not in fonts: fonts[key]=ImageFont.truetype(BOLD if bold else FONT,size)
    return fonts[key]
def text(d,xy,s,size=24,fill=INK,bold=False):d.text(xy,s,font=font(size,bold),fill=fill)
def rr(d,box,fill='#fffefa',radius=20,outline=LINE,width=1):d.rounded_rectangle(box,radius,fill=fill,outline=outline,width=width)
def ease(t):return 1-(1-max(0,min(1,t)))**3
def mouse(d,cx,cy,scale=1):
    x,y=cx-105*scale,cy-142*scale
    rr(d,(x,y,x+210*scale,y+284*scale),'#e5ddd0',round(94*scale),GOLD,3)
    d.line((cx,y+8*scale,cx,y+115*scale),fill=GOLD,width=2)
    d.line((x+10*scale,y+115*scale,x+200*scale,y+115*scale),fill='#cbb99e',width=2)
    rr(d,(cx-10*scale,y+45*scale,cx+10*scale,y+83*scale),GOLD,8,None)
    text(d,(cx-39*scale,y+202*scale),'MOUSE',round(16*scale),MUTED)
def node(d,x,y,title,sub,active=False):
    rr(d,(x,y,x+210,y+115),'#f5eee3' if active else '#fffefa',16,GOLD if active else LINE,2 if active else 1)
    text(d,(x+20,y+22),title,25,INK,True);text(d,(x+20,y+68),sub,16,MUTED)
def dotline(d,x1,x2,y,t):
    d.line((x1,y,x2,y),fill=LINE,width=2)
    x=x1+(x2-x1)*(t%1);d.ellipse((x-5,y-5,x+5,y+5),fill=GOLD)
def draw_scene(index,t):
    im=Image.new('RGB',(W,H),'#f4f1e9');d=ImageDraw.Draw(im)
    # Fine drafting grid and moving orbit echo the factory's planning language.
    for x in range(0,W,64):d.line((x,0,x,H),fill='#ede8dd')
    for y in range(0,H,64):d.line((0,y,W,y),fill='#ede8dd')
    d.ellipse((860,-180,1490,450),outline='#e6dccb',width=2)
    d.ellipse((920,-120,1430,390),outline='#e6dccb',width=1)
    text(d,(60,35),'MOUSE / MES',20,INK,True)
    text(d,(980,39),'制造执行管理平台',16,MUTED)
    d.line((60,80,1220,80),fill=LINE,width=1)
    titles=[('从需求出发，让制造有序发生','一份订单，连接每一个制造环节。'),
            ('把客户需求，变成可执行的工单','订单 · BOM · 工艺路线'),
            ('让每个岗位，接上下一步','排班排产 · 开工报工 · 进度协同'),
            ('让每次交付，都有来源可查','质量检验 · 库存过账 · 批次追溯'),
            ('业务问题，问小栗','本地技能查询，保留条件、时间与来源。'),
            ('让生产井然有序，让协作轻松发生','MOUSE MES  /  连接人、设备与物料')]
    title,sub=titles[index]
    y=115+int(16*(1-ease(t*2)))
    text(d,(60,y),f'0{index+1}  /  '+['需求','计划','执行','交付','协作','连接'][index],17,GOLD,True)
    text(d,(60,y+43),title,40,INK,True);text(d,(62,y+108),sub,21,MUTED)
    if index==0:
        mouse(d,950,422,0.8)
        for i,(a,b) in enumerate([('客户订单','明确交付需求'),('制造工单','组织物料与工艺'),('批次交付','保留来源与记录')]):
            node(d,70+i*238,342,a,b, i==min(2,int(t/1.5)))
            if i<2:dotline(d,280+i*238,308+i*238,400,t*.6-i*.1)
        text(d,(70,510),'以工单为线索，把分散记录连成业务过程。',20,MUTED)
    elif index==1:
        rr(d,(70,305,425,570),radius=18)
        text(d,(96,330),'客户需求',17,GOLD);text(d,(96,377),'A100 有线办公鼠标',26,INK,True)
        d.line((96,430,396,430),fill=LINE,width=1)
        text(d,(96,451),'产品与数量',20);text(d,(96,496),'交付日期与业务来源',20)
        dotline(d,425,510,435,t*.5)
        node(d,515,315,'产品 BOM','明确物料组成',True)
        node(d,515,465,'工艺路线','规定工序顺序',True)
        dotline(d,725,790,435,t*.5)
        node(d,795,385,'制造工单','冻结本次生产依据',True)
    elif index==2:
        stages=['排班与资源','工序任务','开工与报工','进度更新']
        for i,label in enumerate(stages):
            x=70+i*290
            node(d,x,328,label,['人员 · 工作站','明确负责人','记录实际完成量','连接后续工序'][i],i==int(t*.8)%4)
            if i<3:dotline(d,x+210,x+290,385,t*.6)
            rr(d,(x,478,x+210,486),'#e3ddd1',4,None)
            progress=max(0,min(1,(t-i*.5)/2.5))
            if progress:rr(d,(x,478,x+210*progress,486),GOLD,4,None)
        text(d,(73,543),'人、设备与时间，在同一条生产任务中协作。',23,MUTED)
    elif index==3:
        for i,(a,b) in enumerate([('过程检验','实测结果决定放行'),('成品入库','过账形成库存流水'),('包装发货','批次关联交付来源'),('追溯核对','业务记录有据可查')]):
            x=70+i*290;node(d,x,340,a,b,i<=int(t*.7))
            if i<3:dotline(d,x+210,x+290,400,t*.6)
            cx=x+105;d.ellipse((cx-16,496,cx+16,528),fill=SAGE if i<=int(t*.7) else '#ded8cc')
            if i<=int(t*.7):d.line((cx-7,512,cx-1,518,cx+9,506),fill='white',width=3)
        text(d,(73,574),'检验放行与库存过账，分别记录、相互关联。',22,MUTED)
    elif index==4:
        rr(d,(70,307,348,582),'#eae2d5',24)
        rr(d,(126,360,292,462),'#fffefa',38,GOLD,2)
        rr(d,(153,382,266,428),INK,15,None)
        for x in [181,239]:d.ellipse((x-6,399,x+6,411),fill='#d6be90')
        text(d,(159,494),'你好，我是小栗',21,INK,True)
        rr(d,(400,310,1200,385),'#fffefa',16)
        text(d,(425,331),'这张工单生产到哪一步了？',25)
        rr(d,(400,410,1200,568),'#efe8dc',16,None)
        text(d,(425,433),'关联查询',19,GOLD,True)
        for j,label in enumerate(['工单与任务','报工与检验','条件与来源']):
            rr(d,(425+j*248,483,652+j*248,535),'#fffefa',9,None)
            text(d,(445+j*248,496),label,19)
    else:
        mouse(d,640,420,.68)
        for x,label in [(110,'订单与物料'),(945,'生产与质量')]:
            text(d,(x,370),label,28,INK,True)
            text(d,(x,418),'协同 · 留痕 · 可追溯',18,MUTED)
        text(d,(473,570),'进入工作台，开始今天的生产协作。',20,GOLD)
    d.line((60,654,1220,654),fill=LINE,width=1)
    text(d,(60,675),'MOUSE MES  /  产品功能概念短片',14,MUTED)
    for i in range(6):
        x=1040+i*29
        rr(d,(x,681,x+18,685),GOLD if i==index else '#d5cdbf',2,None)
    return im


def main():
    TEMP.mkdir(parents=True,exist_ok=True);OUT.mkdir(parents=True,exist_ok=True)
    ff=imageio_ffmpeg.get_ffmpeg_exe()
    draw_scene(0,2).save(OUT/'mousemes-intro-poster.jpg',quality=92)
    silent=TEMP/'silent.mp4'
    proc=subprocess.Popen([ff,'-y','-loglevel','error','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}',
        '-r',str(FPS),'-i','-','-an','-c:v','libx264','-preset','fast','-crf','22','-pix_fmt','yuv420p',str(silent)],stdin=subprocess.PIPE)
    for frame in range(DURATION*FPS):
        sec=frame/FPS;index=min(5,int(sec/5));local=sec-index*5
        im=draw_scene(index,local)
        # Subtle paper dissolve at chapter boundaries; no flashing cuts.
        fade=min(1,local/.3,(5-local)/.3)
        if fade<1:im=Image.blend(Image.new('RGB',(W,H),'#f4f1e9'),im,max(0,fade))
        proc.stdin.write(im.tobytes())
        if frame%120==0:print(f'Rendered {frame//FPS}/{DURATION}s',flush=True)
    proc.stdin.close();assert proc.wait()==0
    # Original quiet ambient score, synthesized locally (no licensed music assets).
    rate=44100;t=np.arange(rate*DURATION)/rate;signal=np.zeros_like(t)
    for freq in [130.81,196,261.63,329.63]:
        signal+=.025*np.sin(2*np.pi*freq*t+.3*np.sin(t*.4))
    signal*=np.minimum(1,t/2)*np.minimum(1,(DURATION-t)/2)*(.75+.25*np.sin(t*.35))
    music=TEMP/'ambient.wav'
    with wave.open(str(music),'wb') as f:
        f.setnchannels(1);f.setsampwidth(2);f.setframerate(rate);f.writeframes((signal*32767).astype('<i2').tobytes())
    inputs=[ff,'-y','-loglevel','error','-i',str(silent),'-i',str(music)]
    filters=['[1:a]volume=0.45[bed]'];mix=['[bed]']
    for i in range(6):
        source=TEMP/f'voice-{i}.wav'
        if not source.exists() or source.stat().st_size<100:continue
        with wave.open(str(source),'rb') as f:duration=f.getnframes()/f.getframerate()
        speed=max(1,duration/4.55)
        inputs+=['-i',str(source)]
        n=len(mix)+1
        filters.append(f'[{n}:a]atempo={speed:.4f},volume=1.5,adelay={i*5000+220}:all=1[v{i}]')
        mix.append(f'[v{i}]')
    filters.append(''.join(mix)+f'amix=inputs={len(mix)}:normalize=0,alimiter=limit=0.9[a]')
    subprocess.run(inputs+['-filter_complex',';'.join(filters),'-map','0:v','-map','[a]','-c:v','copy',
        '-c:a','aac','-b:a','128k','-t',str(DURATION),'-movflags','+faststart',str(OUT/'mousemes-intro.mp4')],check=True)
    print('Exported',OUT/'mousemes-intro.mp4',flush=True)

if __name__=='__main__':main()
