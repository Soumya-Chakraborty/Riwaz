#!/usr/bin/env python3
"""
TFLite Model Converter
Converts trained Keras models to TensorFlow Lite format for Android deployment
"""

import argparse
import tensorflow as tf
import numpy as np
from pathlib import Path


def convert_to_tflite(model_path, output_path, optimize=True, quantize=None):
    """
    Convert Keras model to TFLite format
    
    Args:
        model_path: Path to .h5 model file
        output_path: Path for output .tflite file
        optimize: Whether to optimize model
        quantize: Quantization type ('int8', 'float16', or None)
    """
    print(f"Loading model from: {model_path}")
    model = tf.keras.models.load_model(model_path)
    
    # Create converter
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # Optimization settings
    if optimize:
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        print("Optimization enabled")
    
    # Quantization settings
    if quantize == 'int8':
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        converter.inference_input_type = tf.int8
        converter.inference_output_type = tf.int8
        print("INT8 quantization enabled")
        
    elif quantize == 'float16':
        converter.target_spec.supported_types = [tf.float16]
        print("Float16 quantization enabled")
    
    # Convert model
    print("Converting model...")
    tflite_model = converter.convert()
    
    # Save model
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    # Print statistics
    original_size = Path(model_path).stat().st_size / (1024 * 1024)  # MB
    tflite_size = len(tflite_model) / (1024 * 1024)  # MB
    compression_ratio = (1 - tflite_size / original_size) * 100
    
    print(f"\nConversion complete!")
    print(f"  Original model size: {original_size:.2f} MB")
    print(f"  TFLite model size: {tflite_size:.2f} MB")
    print(f"  Compression: {compression_ratio:.1f}%")
    print(f"  Output saved to: {output_path}")
    
    return tflite_model


def test_tflite_model(tflite_path, test_input=None):
    """
    Test TFLite model inference
    
    Args:
        tflite_path: Path to .tflite file
        test_input: Optional test input array
    """
    print(f"\nTesting TFLite model: {tflite_path}")
    
    # Load TFLite model
    interpreter = tf.lite.Interpreter(model_path=str(tflite_path))
    interpreter.allocate_tensors()
    
    # Get input and output details
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print("\nModel Details:")
    print(f"  Input shape: {input_details[0]['shape']}")
    print(f"  Input type: {input_details[0]['dtype']}")
    print(f"  Output shape: {output_details[0]['shape']}")
    print(f"  Output type: {output_details[0]['dtype']}")
    
    # Create test input if not provided
    if test_input is None:
        input_shape = input_details[0]['shape']
        test_input = np.random.randn(*input_shape).astype(np.float32)
    
    # Run inference
    interpreter.set_tensor(input_details[0]['index'], test_input)
    interpreter.invoke()
    output = interpreter.get_tensor(output_details[0]['index'])
    
    print(f"\nInference test successful!")
    print(f"  Output shape: {output.shape}")
    print(f"  Output sample: {output[0][:5]}...")  # Show first 5 values
    
    return output


def main():
    parser = argparse.ArgumentParser(description='Convert Keras model to TFLite')
    
    parser.add_argument('--model', type=str, required=True,
                       help='Path to input .h5 model file')
    parser.add_argument('--output', type=str, required=True,
                       help='Path for output .tflite file')
    parser.add_argument('--optimize', action='store_true',
                       help='Enable model optimization')
    parser.add_argument('--quantize', type=str, choices=['int8', 'float16'],
                       help='Quantization type')
    parser.add_argument('--test', action='store_true',
                       help='Test converted model')
    
    args = parser.parse_args()
    
    # Convert model
    tflite_model = convert_to_tflite(
        args.model,
        args.output,
        optimize=args.optimize,
        quantize=args.quantize
    )
    
    # Test if requested
    if args.test:
        test_tflite_model(args.output)
    
    print("\n" + "=" * 60)
    print("Next steps:")
    print(f"1. Test on Android: cp {args.output} ../../app/src/main/assets/")
    print("2. Verify in app logs: 'TFLite model loaded successfully'")
    print("3. Benchmark performance on device")
    print("=" * 60)


if __name__ == '__main__':
    main()
