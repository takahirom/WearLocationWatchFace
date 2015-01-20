adb pull /sdcard/test.raw
ffmpeg -f rawvideo -vcodec rawvideo -s 280x280 -pix_fmt rgb24 -r 20 -i test.raw -an -c:v libx264 -pix_fmt yuv420p test.mp4
