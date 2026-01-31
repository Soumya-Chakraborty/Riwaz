#!/usr/bin/env python3
"""
Swar Classification Model Training Script
Trains a CNN for classifying 12 swars from mel spectrograms
"""

import os
import argparse
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from pathlib import Path
import json


def create_swar_classifier(input_shape=(128, 128, 1), num_classes=12):
    """
    Create CNN model for swar classification
    
    Args:
        input_shape: Shape of mel spectrogram input
        num_classes: Number of swar classes (12)
    
    Returns:
        Keras model
    """
    model = keras.Sequential([
        # Input layer
        layers.Input(shape=input_shape),
        
        # First conv block
        layers.Conv2D(32, (3, 3), activation='relu', padding='same'),
        layers.BatchNormalization(),
        layers.MaxPooling2D((2, 2)),
        layers.Dropout(0.25),
        
        # Second conv block
        layers.Conv2D(64, (3, 3), activation='relu', padding='same'),
        layers.BatchNormalization(),
        layers.MaxPooling2D((2, 2)),
        layers.Dropout(0.25),
        
        # Third conv block
        layers.Conv2D(128, (3, 3), activation='relu', padding='same'),
        layers.BatchNormalization(),
        layers.GlobalAveragePooling2D(),
        
        # Fully connected layers
        layers.Dense(128, activation='relu'),
        layers.Dropout(0.5),
        
        # Output layer - 12 swar classes
        layers.Dense(num_classes, activation='softmax', name='swar_output')
    ], name='swar_classifier')
    
    return model


def load_dataset(data_dir):
    """
    Load swar classification dataset
    
    Expected structure:
        data_dir/
            Sa/
                sample_001.npy (mel spectrogram)
                sample_002.npy
            Re_komal/
            Re/
            ... (12 folders total)
    """
    swar_classes = [
        'Sa', 'Re_komal', 'Re', 'Ga_komal', 'Ga', 'Ma',
        'Ma_tivra', 'Pa', 'Dha_komal', 'Dha', 'Ni_komal', 'Ni'
    ]
    
    X = []
    y = []
    
    data_path = Path(data_dir)
    
    for class_idx, swar_class in enumerate(swar_classes):
        class_dir = data_path / swar_class
        
        if not class_dir.exists():
            print(f"Warning: {swar_class} directory not found")
            continue
        
        # Load all .npy files (mel spectrograms)
        for spec_file in class_dir.glob('*.npy'):
            try:
                # Load mel spectrogram
                mel_spec = np.load(spec_file)
                
                # Ensure correct shape (128, 128)
                if mel_spec.shape != (128, 128):
                    continue
                
                X.append(mel_spec)
                y.append(class_idx)
                
            except Exception as e:
                print(f"Error loading {spec_file}: {e}")
                continue
    
    if len(X) == 0:
        # Create dummy data for testing
        print("No data found, creating dummy dataset for testing...")
        for i in range(1000):
            X.append(np.random.randn(128, 128))
            y.append(i % 12)
    
    X = np.array(X).reshape(-1, 128, 128, 1)
    y = np.array(y)
    
    return X, y


def train_model(args):
    """Main training function"""
    
    print("=" * 60)
    print("Swar Classification Model Training")
    print("=" * 60)
    
    # Load datasets
    print("\n[1/5] Loading datasets...")
    X_train, y_train = load_dataset(Path(args.data_dir) / 'train')
    X_val, y_val = load_dataset(Path(args.data_dir) / 'val')
    
    print(f"  Train samples: {len(X_train)}")
    print(f"  Val samples: {len(X_val)}")
    print(f"  Classes: 12 swars")
    
    # Normalize data
    X_train = X_train.astype('float32') / 255.0
    X_val = X_val.astype('float32') / 255.0
    
    # Create model
    print("\n[2/5] Creating model...")
    model = create_swar_classifier()
    model.summary()
    
    # Compile model
    print("\n[3/5] Compiling model...")
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=args.learning_rate),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy', keras.metrics.TopKCategoricalAccuracy(k=3, name='top3_acc')]
    )
    
    # Callbacks
    callbacks = [
        keras.callbacks.ModelCheckpoint(
            filepath=os.path.join(args.checkpoint_dir, 'swar_classifier_best.h5'),
            monitor='val_accuracy',
            save_best_only=True,
            verbose=1
        ),
        keras.callbacks.TensorBoard(
            log_dir=args.tensorboard_dir,
            histogram_freq=1
        ),
        keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=5,
            min_lr=1e-6,
            verbose=1
        )
    ]
    
    if args.early_stopping:
        callbacks.append(
            keras.callbacks.EarlyStopping(
                monitor='val_loss',
                patience=args.patience,
                restore_best_weights=True,
                verbose=1
            )
        )
    
    # Train model
    print("\n[4/5] Training model...")
    history = model.fit(
        X_train, y_train,
        batch_size=args.batch_size,
        epochs=args.epochs,
        validation_data=(X_val, y_val),
        callbacks=callbacks,
        verbose=1
    )
    
    # Save final model
    print("\n[5/5] Saving model...")
    model_path = Path(args.output_dir) / 'swar_classifier.h5'
    model.save(model_path)
    print(f"  Model saved to: {model_path}")
    
    # Save training history
    history_path = Path(args.output_dir) / 'swar_training_history.json'
    with open(history_path, 'w') as f:
        json.dump(history.history, f, indent=2)
    print(f"  History saved to: {history_path}")
    
    print("\n" + "=" * 60)
    print("Training completed successfully!")
    print("=" * 60)
    
    return model, history


def main():
    parser = argparse.ArgumentParser(description='Train swar classification model')
    
    # Data arguments
    parser.add_argument('--data-dir', type=str, default='../data/swar_dataset',
                       help='Path to dataset directory')
    
    # Training arguments
    parser.add_argument('--epochs', type=int, default=50,
                       help='Number of training epochs')
    parser.add_argument('--batch-size', type=int, default=64,
                       help='Batch size for training')
    parser.add_argument('--learning-rate', type=float, default=0.001,
                       help='Learning rate')
    
    # Augmentation
    parser.add_argument('--augmentation', action='store_true',
                       help='Enable data augmentation')
    parser.add_argument('--mixup', type=float, default=0.0,
                       help='Mixup alpha (0 to disable)')
    parser.add_argument('--spec-augment', action='store_true',
                       help='Enable SpecAugment')
    
    # Regularization
    parser.add_argument('--early-stopping', action='store_true',
                       help='Enable early stopping')
    parser.add_argument('--patience', type=int, default=10,
                       help='Early stopping patience')
    
    # Output arguments
    parser.add_argument('--output-dir', type=str, default='../models',
                       help='Output directory for trained model')
    parser.add_argument('--checkpoint-dir', type=str, default='checkpoints',
                       help='Directory for model checkpoints')
    parser.add_argument('--tensorboard-dir', type=str, default='logs',
                       help='Directory for TensorBoard logs')
    
    args = parser.parse_args()
    
    # Create output directories
    Path(args.output_dir).mkdir(parents=True, exist_ok=True)
    Path(args.checkpoint_dir).mkdir(parents=True, exist_ok=True)
    Path(args.tensorboard_dir).mkdir(parents=True, exist_ok=True)
    
    # Train model
    model, history = train_model(args)
    
    print("\nNext steps:")
    print("1. Evaluate model: python ../utils/evaluation.py --model ../models/swar_classifier.h5")
    print("2. Convert to TFLite: python ../utils/tflite_converter.py --model ../models/swar_classifier.h5")
    print("3. Deploy to Android: cp ../models/swar_classifier.tflite ../../app/src/main/assets/")


if __name__ == '__main__':
    main()
