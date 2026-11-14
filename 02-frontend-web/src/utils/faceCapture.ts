export async function captureFaceFrames(video: HTMLVideoElement, cancelled: () => boolean) {
    const canvas = document.createElement('canvas')
    canvas.width = 640
    canvas.height = 480
    const context = canvas.getContext('2d')
    if (!context) throw new Error('无法读取摄像头画面')
    const frames: string[] = []
    for (let i = 0; i < 5; i++) {
      await new Promise(resolve => setTimeout(resolve, 600))
      if (cancelled()) throw new Error("摄像头采集已取消")
      context.drawImage(video, 0, 0, canvas.width, canvas.height)
      frames.push(canvas.toDataURL('image/jpeg', 0.75).split(',')[1])
    }
    return frames
}
