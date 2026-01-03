/**
* ImageCompressor - Utility for image compression on the frontend
*
* Features:
* - Resizes images while maintaining aspect ratio
* - Compresses to JPEG format with adjustable quality
* - Batch processing
* - Safe fallback in case of error
*/

export interface CompressionOptions {
  maxWidth?: number;      // Maximum width (default: 1920)
  maxHeight?: number;     // Maximum height
  quality?: number;       // JPEG quality (0.1 to 1.0, default: 0.8)
  outputFormat?: string;  // Output format (default: 'image/jpeg')
  maintainAspectRatio?: boolean; // Maintain aspect ratio (default: true)
}

export interface CompressionResult {
  file: File;                    // Compressed file
  originalSize: number;          // Original size in bytes
  compressedSize: number;        // Compressed size in bytes
  reductionPercentage: number;   // Percentage reduction
  width: number;                 // Resulting width
  height: number;                // Resulting height
}

export class ImageCompressor {
  
  /**
   * Compresses a single image.
   */
  static async compress(
    file: File, 
    options: CompressionOptions = {}
  ): Promise<CompressionResult> {
    const {
      maxWidth = 1920,
      maxHeight = undefined,
      quality = 0.8,
      outputFormat = 'image/jpeg',
      maintainAspectRatio = true
    } = options;

    // Validate input
    if (!file || !file.type.startsWith('image/')) {
      throw new Error('Arquivo inválido ou não é uma imagem');
    }

    // If the image is already small, it will be returned without compression.
    if (file.size <= 300 * 1024) { // 300KB
      return {
        file,
        originalSize: file.size,
        compressedSize: file.size,
        reductionPercentage: 0,
        width: 0,
        height: 0
      };
    }

    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      const img = new Image();
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');

      if (!ctx) {
        reject(new Error('Canvas context não disponível'));
        return;
      }

      reader.onload = (e) => {
        if (!e.target?.result) {
          reject(new Error('Falha ao ler imagem'));
          return;
        }

        img.onload = () => {
          try {
            // Calculate new dimensions
            let { width, height } = this.calculateDimensions(
              img.width, 
              img.height, 
              maxWidth, 
              maxHeight, 
              maintainAspectRatio
            );

            // Configure canvas
            canvas.width = width;
            canvas.height = height;

            // Configure rendering quality
            ctx.imageSmoothingEnabled = true;
            ctx.imageSmoothingQuality = 'high';

            // Draw a resized image.
            ctx.drawImage(img, 0, 0, width, height);

            // Convert to Blob
            canvas.toBlob(
              (blob) => {
                if (!blob) {
                  reject(new Error('Falha na compressão'));
                  return;
                }

                // Create new file
                const compressedFile = new File(
                  [blob],
                  this.generateFileName(file.name),
                  {
                    type: outputFormat,
                    lastModified: Date.now(),
                  }
                );

                // Calculate statistics
                const reduction = ((1 - compressedFile.size / file.size) * 100);

                resolve({
                  file: compressedFile,
                  originalSize: file.size,
                  compressedSize: compressedFile.size,
                  reductionPercentage: parseFloat(reduction.toFixed(1)),
                  width,
                  height
                });
              },
              outputFormat,
              quality
            );
          } catch (error) {
            reject(error);
          }
        };

        img.onerror = () => {
          reject(new Error('Falha ao carregar imagem'));
        };

        img.src = e.target.result as string;
      };

      reader.onerror = () => {
        reject(new Error('Falha ao ler arquivo'));
      };

      reader.readAsDataURL(file);
    });
  }

  /**
   * Compresses multiple images in parallel.
   */
  static async compressAll(
    files: File[], 
    options: CompressionOptions = {}
  ): Promise<CompressionResult[]> {
    console.log(`🔄 Comprimindo ${files.length} imagem(ns)`);

    const promises = files.map(async (file) => {
      try {
        return await this.compress(file, options);
      } catch (error) {
        console.warn(`⚠️ Falha na compressão de ${file.name}, usando original:`, error);
        
        // Fallback: restores the original file.
        return {
          file,
          originalSize: file.size,
          compressedSize: file.size,
          reductionPercentage: 0,
          width: 0,
          height: 0
        };
      }
    });

    const results = await Promise.all(promises);
    
    // Log
    this.logCompressionSummary(results);
    
    return results;
  }

  /**
   * Calculates dimensions while maintaining proportion
   */
  private static calculateDimensions(
    originalWidth: number,
    originalHeight: number,
    maxWidth: number,
    maxHeight?: number,
    maintainAspectRatio = true
  ): { width: number; height: number } {
    let width = originalWidth;
    let height = originalHeight;

    // Resize by width if necessary.
    if (width > maxWidth) {
      if (maintainAspectRatio) {
        height = Math.round((height * maxWidth) / width);
        width = maxWidth;
      } else {
        width = maxWidth;
      }
    }

    // Resize by height if specified.
    if (maxHeight && height > maxHeight) {
      if (maintainAspectRatio) {
        width = Math.round((width * maxHeight) / height);
        height = maxHeight;
      } else {
        height = maxHeight;
      }
    }

    return { width, height };
  }

  /**
   * Generates a filename for the compressed version.
   */
  private static generateFileName(originalName: string): string {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8);
    const nameWithoutExt = originalName.replace(/\.[^/.]+$/, '');
    const extension = 'jpg'; // Sempre JPEG para imagens comprimidas
    
    return `compressed_${nameWithoutExt}_${timestamp}_${random}.${extension}`;
  }

  /**
   * Compression summary log
   */
  private static logCompressionSummary(results: CompressionResult[]): void {
    const totalOriginal = results.reduce((sum, r) => sum + r.originalSize, 0);
    const totalCompressed = results.reduce((sum, r) => sum + r.compressedSize, 0);
    const totalReduction = ((1 - totalCompressed / totalOriginal) * 100);
    
    console.log('✅ Compressão concluída!');
    console.log(`   📊 Resumo:`);
    console.log(`      • Imagens processadas: ${results.length}`);
    console.log(`      • Tamanho total original: ${(totalOriginal / 1024 / 1024).toFixed(2)}MB`);
    console.log(`      • Tamanho total comprimido: ${(totalCompressed / 1024 / 1024).toFixed(2)}MB`);
    console.log(`      • Redução total: ${totalReduction.toFixed(1)}%`);
    
    // Individual log for images with good reduction.
    results.forEach(result => {
      if (result.reductionPercentage > 20) {
        console.log(`      🎉 ${result.file.name}: ${result.reductionPercentage}% de redução`);
      }
    });
  }

  /**
   * Checks if the file is a supported image.
   */
  static isSupportedImage(file: File): boolean {
    const supportedTypes = [
      'image/jpeg',
      'image/jpg',
      'image/png',
      'image/webp',
      'image/gif'
    ];
    
    return supportedTypes.includes(file.type);
  }

  /**
   * Get the image dimensions without fully loading it.
   */
  static async getImageDimensions(file: File): Promise<{width: number, height: number}> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const reader = new FileReader();
      
      reader.onload = (e) => {
        img.onload = () => {
          resolve({ width: img.width, height: img.height });
        };
        
        img.onerror = () => {
          reject(new Error('Falha ao carregar imagem para obter dimensões'));
        };
        
        img.src = e.target?.result as string;
      };
      
      reader.onerror = () => {
        reject(new Error('Falha ao ler arquivo'));
      };
      
      reader.readAsDataURL(file);
    });
  }
}