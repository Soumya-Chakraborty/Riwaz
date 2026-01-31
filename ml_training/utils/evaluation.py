#!/usr/bin/env python3
"""
Model Evaluation Utilities
Comprehensive evaluation for pitch detection and swar classification models
"""

import argparse
import numpy as np
import tensorflow as tf
from pathlib import Path
import json
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt
import seaborn as sns


def evaluate_pitch_model(model_path, test_data_dir):
    """
    Evaluate pitch detection model
    
    Metrics:
    - Raw Pitch Accuracy (RPA): % within ±50 cents
    - Gross Pitch Error (GPE): % with error > 100 cents
    - Mean Absolute Error (MAE) in cents
    """
    print("Evaluating Pitch Detection Model...")
    print("=" * 60)
    
    # Load model
    if model_path.endswith('.tflite'):
        interpreter = tf.lite.Interpreter(model_path=model_path)
        interpreter.allocate_tensors()
        is_tflite = True
    else:
        model = tf.keras.models.load_model(model_path)
        is_tflite = False
    
    # Load test data (placeholder)
    # In real implementation, load actual test data
    test_samples = 100
    predictions = []
    ground_truth = []
    
    for i in range(test_samples):
        # Dummy data
        audio = np.random.randn(1, 1024, 1).astype(np.float32)
        true_pitch = 200.0 + np.random.randn() * 50  # Hz
        
        # Predict
        if is_tflite:
            input_details = interpreter.get_input_details()
            output_details = interpreter.get_output_details()
            interpreter.set_tensor(input_details[0]['index'], audio)
            interpreter.invoke()
            output = interpreter.get_tensor(output_details[0]['index'])
            pred_pitch = output[0].argmax()  # Simplified
        else:
            output = model.predict(audio, verbose=0)
            pred_pitch = output[0].argmax()  # Simplified
        
        predictions.append(pred_pitch)
        ground_truth.append(true_pitch)
    
    # Calculate metrics
    predictions = np.array(predictions)
    ground_truth = np.array(ground_truth)
    
    # Convert to cents difference (simplified)
    cents_diff = np.abs(predictions - ground_truth)
    
    rpa_50 = np.mean(cents_diff < 50) * 100
    gpe = np.mean(cents_diff > 100) * 100
    mae = np.mean(cents_diff)
    
    results = {
        'model_type': 'pitch_detection',
        'test_samples': test_samples,
        'rpa_50_cents': rpa_50,
        'gross_pitch_error': gpe,
        'mae_cents': mae
    }
    
    print(f"\nResults:")
    print(f"  Test Samples: {test_samples}")
    print(f"  RPA (±50 cents): {rpa_50:.2f}%")
    print(f"  Gross Pitch Error: {gpe:.2f}%")
    print(f"  MAE (cents): {mae:.2f}")
    
    return results


def evaluate_swar_classifier(model_path, test_data_dir):
    """
    Evaluate swar classification model
    
    Metrics:
    - Top-1 Accuracy
    - Top-3 Accuracy
    - Per-class F1 score
    - Confusion matrix
    """
    print("Evaluating Swar Classification Model...")
    print("=" * 60)
    
    swar_classes = [
        'Sa', 'Re(k)', 'Re', 'Ga(k)', 'Ga', 'Ma',
        'Ma(t)', 'Pa', 'Dha(k)', 'Dha', 'Ni(k)', 'Ni'
    ]
    
    # Load model
    if model_path.endswith('.tflite'):
        interpreter = tf.lite.Interpreter(model_path=model_path)
        interpreter.allocate_tensors()
        is_tflite = True
    else:
        model = tf.keras.models.load_model(model_path)
        is_tflite = False
    
    # Load test data (placeholder)
    test_samples = 120  # 10 per class
    predictions = []
    ground_truth = []
    
    for i in range(test_samples):
        # Dummy data
        mel_spec = np.random.randn(1, 128, 128, 1).astype(np.float32)
        true_class = i % 12
        
        # Predict
        if is_tflite:
            input_details = interpreter.get_input_details()
            output_details = interpreter.get_output_details()
            interpreter.set_tensor(input_details[0]['index'], mel_spec)
            interpreter.invoke()
            output = interpreter.get_tensor(output_details[0]['index'])
        else:
            output = model.predict(mel_spec, verbose=0)
        
        pred_class = output[0].argmax()
        predictions.append(pred_class)
        ground_truth.append(true_class)
    
    predictions = np.array(predictions)
    ground_truth = np.array(ground_truth)
    
    # Calculate metrics
    top1_acc = np.mean(predictions == ground_truth) * 100
    
    # Classification report
    report = classification_report(
        ground_truth, predictions,
        target_names=swar_classes,
        output_dict=True,
        zero_division=0
    )
    
    # Confusion matrix
    cm = confusion_matrix(ground_truth, predictions)
    
    results = {
        'model_type': 'swar_classification',
        'test_samples': test_samples,
        'top1_accuracy': top1_acc,
        'classification_report': report,
        'confusion_matrix': cm.tolist()
    }
    
    print(f"\nResults:")
    print(f"  Test Samples: {test_samples}")
    print(f"  Top-1 Accuracy: {top1_acc:.2f}%")
    print(f"\nPer-class F1 Scores:")
    for swar in swar_classes:
        if swar in report:
            f1 = report[swar]['f1-score']
            print(f"    {swar:8s}: {f1:.3f}")
    
    return results


def plot_confusion_matrix(cm, class_names, output_path):
    """Plot and save confusion matrix"""
    plt.figure(figsize=(12, 10))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
                xticklabels=class_names, yticklabels=class_names)
    plt.title('Confusion Matrix')
    plt.ylabel('True Label')
    plt.xlabel('Predicted Label')
    plt.tight_layout()
    plt.savefig(output_path)
    print(f"\nConfusion matrix saved to: {output_path}")


def main():
    parser = argparse.ArgumentParser(description='Evaluate ML models')
    
    parser.add_argument('--model', type=str, required=True,
                       help='Path to model file (.h5 or .tflite)')
    parser.add_argument('--test-data', type=str, required=True,
                       help='Path to test data directory')
    parser.add_argument('--model-type', type=str,
                       choices=['pitch', 'swar', 'auto'],
                       default='auto',
                       help='Type of model to evaluate')
    parser.add_argument('--output-report', type=str, default='evaluation_report.json',
                       help='Output path for evaluation report')
    parser.add_argument('--confusion-matrix', action='store_true',
                       help='Generate confusion matrix plot')
    
    args = parser.parse_args()
    
    # Auto-detect model type
    if args.model_type == 'auto':
        if 'pitch' in args.model.lower():
            model_type = 'pitch'
        elif 'swar' in args.model.lower():
            model_type = 'swar'
        else:
            print("Cannot auto-detect model type. Please specify --model-type")
            return
    else:
        model_type = args.model_type
    
    # Evaluate model
    if model_type == 'pitch':
        results = evaluate_pitch_model(args.model, args.test_data)
    else:
        results = evaluate_swar_classifier(args.model, args.test_data)
        
        # Plot confusion matrix if requested
        if args.confusion_matrix and 'confusion_matrix' in results:
            swar_classes = [
                'Sa', 'Re(k)', 'Re', 'Ga(k)', 'Ga', 'Ma',
                'Ma(t)', 'Pa', 'Dha(k)', 'Dha', 'Ni(k)', 'Ni'
            ]
            cm = np.array(results['confusion_matrix'])
            plot_confusion_matrix(cm, swar_classes, 'confusion_matrix.png')
    
    # Save results
    with open(args.output_report, 'w') as f:
        # Convert numpy arrays to lists for JSON serialization
        json_results = {}
        for key, value in results.items():
            if isinstance(value, np.ndarray):
                json_results[key] = value.tolist()
            else:
                json_results[key] = value
        json.dump(json_results, f, indent=2)
    
    print(f"\nEvaluation report saved to: {args.output_report}")
    print("=" * 60)


if __name__ == '__main__':
    main()
