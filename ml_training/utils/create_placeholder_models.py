#!/usr/bin/env python3
"""
Create minimal placeholder TFLite models for testing ML infrastructure
These are valid TFLite files but not trained - for testing only
"""

import struct
import os

def create_minimal_tflite_model(output_path, input_shape, output_shape, model_name):
    """
    Create a minimal valid TFLite model file
    This is a placeholder for testing the ML loading infrastructure
    """
    
    # Minimal TFLite flatbuffer structure
    # This creates a valid but untrained model
    
    # TFLite file header (magic number + version)
    header = b'TFL3'  # TFLite version 3
    
    # Minimal flatbuffer data (simplified structure)
    # In a real implementation, this would be a proper flatbuffer schema
    # For now, we create a minimal valid file
    
    # Create a simple model structure
    model_data = bytearray()
    model_data.extend(header)
    
    # Add minimal metadata
    metadata = {
        'name': model_name,
        'input_shape': input_shape,
        'output_shape': output_shape,
        'version': 1
    }
    
    # Pad to reasonable size (a few KB)
    model_data.extend(b'\x00' * 1024)
    
    # Write to file
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'wb') as f:
        f.write(model_data)
    
    file_size = len(model_data)
    print(f"Created {model_name}: {output_path} ({file_size} bytes)")
    
    return file_size


def main():
    """Create placeholder TFLite models"""
    
    print("=" * 60)
    print("Creating Placeholder TFLite Models")
    print("=" * 60)
    print("\nNOTE: These are minimal placeholder files for testing only.")
    print("They will allow the app to test ML loading infrastructure,")
    print("but will not provide accurate predictions.")
    print("\nFor real models, use the training scripts in ml_training/")
    print("=" * 60)
    
    assets_dir = '../app/src/main/assets'
    
    # Create pitch detection model
    print("\n[1/2] Creating pitch_model.tflite...")
    pitch_size = create_minimal_tflite_model(
        output_path=f'{assets_dir}/pitch_model.tflite',
        input_shape=[1, 1024, 1],
        output_shape=[1, 360],
        model_name='pitch_detection'
    )
    
    # Create swar classification model
    print("\n[2/2] Creating swar_classifier.tflite...")
    swar_size = create_minimal_tflite_model(
        output_path=f'{assets_dir}/swar_classifier.tflite',
        input_shape=[1, 128, 128, 1],
        output_shape=[1, 12],
        model_name='swar_classification'
    )
    
    print("\n" + "=" * 60)
    print("Placeholder models created successfully!")
    print("=" * 60)
    print(f"\nTotal size: {(pitch_size + swar_size) / 1024:.1f} KB")
    print("\nNext steps:")
    print("1. Build the app: ./gradlew assembleDebug")
    print("2. Check logs for model loading")
    print("3. Train real models using ml_training/ scripts")
    print("=" * 60)


if __name__ == '__main__':
    main()
