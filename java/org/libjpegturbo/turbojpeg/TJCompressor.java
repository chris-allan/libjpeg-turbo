/*
 * Copyright (C)2011 D. R. Commander.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the libjpeg-turbo Project nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS",
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.libjpegturbo.turbojpeg;

import java.awt.image.*;

public class TJCompressor {

  public TJCompressor() throws Exception {
    init();
  }

  public TJCompressor(byte [] buf, int width, int pitch, int height,
    int pixelFormat) throws Exception {
    setBitmapBuffer(buf, width, pitch, height, pixelFormat);
  }

  public void setBitmapBuffer(byte [] buf, int width, int pitch, int height,
    int pixelFormat) throws Exception {
    if(handle == 0) init();
    if(buf == null || width < 1 || height < 1 || pitch < 0 || pixelFormat < 0
      || pixelFormat >= TJ.NUMPFOPT)
      throw new Exception("Invalid argument in setBitmapBuffer()");
    bitmapBuf = buf;
    bitmapWidth = width;
    if(pitch == 0) bitmapPitch = width * TJ.getPixelSize(pixelFormat);
    else bitmapPitch = pitch;
    bitmapHeight = height;
    bitmapPixelFormat = pixelFormat;
  }

  public int compress(byte [] dstBuf, int jpegSubsamp, int jpegQual,
    int flags) throws Exception {
    return compress(bitmapBuf, bitmapWidth, bitmapPitch, bitmapHeight,
      TJ.getPixelSize(bitmapPixelFormat), dstBuf, jpegSubsamp, jpegQual,
        flags | TJ.getFlags(bitmapPixelFormat));
  }

  public int compress(BufferedImage srcImage, byte [] dstBuf, int jpegSubsamp,
    int jpegQual, int flags) throws Exception {
    if(srcImage == null || flags < 0)
      throw new Exception("Invalid argument in decompress()");
    flags &= ~(TJ.YUV);
    int width = srcImage.getWidth();
    int height = srcImage.getHeight();
    int pixelFormat;  boolean intPixels=false;
    switch(srcImage.getType()) {
      case BufferedImage.TYPE_3BYTE_BGR:
        pixelFormat=TJ.PF_BGR;  break;
      case BufferedImage.TYPE_BYTE_GRAY:
        pixelFormat=TJ.PF_GRAY;  break;
      case BufferedImage.TYPE_INT_BGR:
        pixelFormat=TJ.PF_RGBX;  intPixels=true;  break;
      case BufferedImage.TYPE_INT_RGB:
        pixelFormat=TJ.PF_BGRX;  intPixels=true;  break;
      default:
        throw new Exception("Unsupported BufferedImage format");
    }
    WritableRaster wr = srcImage.getRaster();
    if(intPixels) {
      SinglePixelPackedSampleModel sm =
        (SinglePixelPackedSampleModel)srcImage.getSampleModel();
      int pitch = sm.getScanlineStride();
      DataBufferInt db = (DataBufferInt)wr.getDataBuffer();
      int [] buf = db.getData();
      return compress(buf, width, pitch, height, dstBuf, jpegSubsamp, jpegQual,
        flags | TJ.getFlags(pixelFormat));
    }
    else {
      ComponentSampleModel sm =
        (ComponentSampleModel)srcImage.getSampleModel();
      int pixelSize = sm.getPixelStride();
      if(pixelSize != TJ.pixelSize[pixelFormat])
        throw new Exception("Inconsistency between pixel format and pixel size in BufferedImage");
      int pitch = sm.getScanlineStride();
      DataBufferByte db = (DataBufferByte)wr.getDataBuffer();
      byte [] buf = db.getData();
      return compress(buf, width, pitch, height, TJ.getPixelSize(pixelFormat),
        dstBuf, jpegSubsamp, jpegQual, flags | TJ.getFlags(pixelFormat));
    }
  }

  public void close() throws Exception {
    destroy();
  }

  protected void finalize() throws Throwable {
    try {
      close();
    } catch(Exception e) {
    }
    finally {
      super.finalize();
    }
  };

  private native void init() throws Exception;

  private native void destroy() throws Exception;

  // JPEG size in bytes is returned
  private native int compress(byte [] srcBuf, int width, int pitch,
    int height, int pixelSize, byte [] dstbuf, int jpegSubsamp, int jpegQual,
    int flags) throws Exception;

  private native int compress(int [] srcBuf, int width, int pitch,
    int height, byte [] dstbuf, int jpegSubsamp, int jpegQual, int flags)
    throws Exception;

  static {
    System.loadLibrary("turbojpeg");
  }

  private long handle = 0;
  private byte [] bitmapBuf = null;
  private int bitmapWidth = 0;
  private int bitmapHeight = 0;
  private int bitmapPitch = 0;
  private int bitmapPixelFormat = -1;
};
