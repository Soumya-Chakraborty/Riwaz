#!/usr/bin/env python3
"""
Download pre-trained TFLite models from TensorFlow Hub
"""

import urllib.request
import os
import sys

# Model definitions
MODELS = {
    'spice': {
        'url': 'https://storage.googleapis.com/tfhub-lite-models/google/lite-model/spice/1.tflite',
        'output': '../../app/src/main/assets/spice_model.tflite',
        'description': 'SPICE pitch detection model (Google/TensorFlow Hub)',
        'size_mb': 10.5
    }
}

def download_model(name, info):
    """Download a single model"""
    print(f"\n[{name.upper()}] {info['description']}")
    print(f"  URL: {info['url']}")
    print(f"  Expected size: ~{info['size_mb']} MB")
    
    output_path = info['output']
    
    # Create directory if needed
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    # Check if already exists
    if os.path.exists(output_path):
        size_mb = os.path.getsize(output_path) / (1024 * 1024)
        print(f"  ⚠ File already exists ({size_mb:.2f} MB)")
        response = input("  Overwrite? (y/n): ")
        if response.lower() != 'y':
            print("  ⏭ Skipped")
            return False
    
    # Download
    try:
        print("  📥 Downloading...")
        urllib.request.urlretrieve(info['url'], output_path)
        
        # Verify
        size_mb = os.path.getsize(output_path) / (1024 * 1024)
        print(f"  ✅ Downloaded successfully!")
        print(f"  📁 Location: {output_path}")
        print(f"  📊 Size: {size_mb:.2f} MB")
        return True
        
    except Exception as e:
        print(f"  ❌ Download failed: {e}")
        return False


def main():
    """Main download function"""
    print("=" * 60)
    print("Pre-trained TFLite Model Downloader")
    print("=" * 60)
    
    success_count = 0
    total_size = 0
    
    for name, info in MODELS.items():
        if download_model(name, info):
            success_count += 1
            if os.path.exists(info['output']):
                total_size += os.path.getsize(info['output'])
    
    print("\n" + "=" * 60)
    print(f"Download Summary")
    print("=" * 60)
    print(f"  Models downloaded: {success_count}/{len(MODELS)}")
    print(f"  Total size: {total_size / (1024 * 1024):.2f} MB")
    
    if success_count > 0:
        print("\n✅ Next steps:")
        print("  1. Verify models in app/src/main/assets/")
        print("  2. Update TFLitePitchDetector.kt to use spice_model.tflite")
        print("  3. Build and test the app")
    
    print("=" * 60)
    
    return 0 if success_count == len(MODELS) else 1


if __name__ == '__main__':
    sys.exit(main())
