#!/usr/bin/env python3
"""
Pitch Detection Model Training Script
Trains a CREPE-inspired CNN for pitch detection in Indian classical music
"""

import os
import argparse
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import pandas as pd
from pathlib import Path
import json

def create_pitch_model(input_shape=(1024, 1), num_pitch_bins=360):
    """
    Create CREPE-inspired pitch detection model
    
    Args:
        input_shape: Shape of input audio (samples, channels)
        num_pitch_bins: Number of pitch bins (50-500 Hz range)
    
    Returns:
        Keras model
    """
    model = keras.Sequential([
        # Input layer
        layers.Input(shape=input_shape),
        
        # First conv block
        layers.Conv1D(filters=1024, kernel_size=512, strides=4, 
                     padding='same', activation='relu'),
        layers.BatchNormalization(),
        layers.Dropout(0.25),
        
        # Second conv block
        layers.Conv1D(filters=128, kernel_size=64, strides=1,
                     padding='same', activation='relu'),
        layers.BatchNormalization(),
        
        # Third conv block
        layers.Conv1D(filters=128, kernel_size=64, strides=1,
                     padding='same', activation='relu'),
        layers.BatchNormalization(),
        
        # Pooling and regularization
        layers.MaxPooling1D(pool_size=2),
        layers.Dropout(0.25),
        
        # Fully connected layers
        layers.Flatten(),
        layers.Dense(256, activation='relu'),
        layers.Dropout(0.25),
        
        # Output layer - pitch classification
        layers.Dense(num_pitch_bins, activation='softmax', name='pitch_output')
    ], name='pitch_detection_model')
    
    return model


def pitch_to_bin(pitch_hz, min_pitch=50.0, max_pitch=500.0, num_bins=360):
    """Convert pitch in Hz to bin index"""
    if pitch_hz < min_pitch or pitch_hz > max_pitch:
        return -1  # Out of range
    
    # Logarithmic binning for better resolution at lower frequencies
    log_min = np.log2(min_pitch)
    log_max = np.log2(max_pitch)
    log_pitch = np.log2(pitch_hz)
    
    bin_idx = int((log_pitch - log_min) / (log_max - log_min) * num_bins)
    return min(bin_idx, num_bins - 1)


def bin_to_pitch(bin_idx, min_pitch=50.0, max_pitch=500.0, num_bins=360):
    """Convert bin index to pitch in Hz"""
    log_min = np.log2(min_pitch)
    log_max = np.log2(max_pitch)
    
    log_pitch = log_min + (bin_idx / num_bins) * (log_max - log_min)
    return 2 ** log_pitch


def load_dataset(data_dir, sample_rate=16000, window_size=1024):
    """
    Load pitch detection dataset
    
    Expected structure:
        data_dir/
            audio/
                sample_0001.wav
                sample_0002.wav
            labels.csv (columns: filename, pitch_hz, confidence)
    """
    audio_dir = Path(data_dir) / 'audio'
    labels_file = Path(data_dir) / 'labels.csv'
    
    # Load labels
    labels_df = pd.read_csv(labels_file)
    
    X = []
    y = []
    
    for idx, row in labels_df.iterrows():
        audio_path = audio_dir / row['filename']
        
        if not audio_path.exists():
            continue
        
        # Load audio (placeholder - would use librosa in real implementation)
        # audio, sr = librosa.load(audio_path, sr=sample_rate)
        
        # For now, create dummy data
        audio = np.random.randn(window_size)
        
        # Convert pitch to bin
        pitch_bin = pitch_to_bin(row['pitch_hz'])
        
        if pitch_bin >= 0:
            X.append(audio)
            y.append(pitch_bin)
    
    X = np.array(X).reshape(-1, window_size, 1)
    y = np.array(y)
    
    return X, y


def train_model(args):
    """Main training function"""
    
    print("=" * 60)
    print("Pitch Detection Model Training")
    print("=" * 60)
    
    # Load datasets
    print("\n[1/5] Loading datasets...")
    X_train, y_train = load_dataset(Path(args.data_dir) / 'train')
    X_val, y_val = load_dataset(Path(args.data_dir) / 'val')
    
    print(f"  Train samples: {len(X_train)}")
    print(f"  Val samples: {len(X_val)}")
    
    # Create model
    print("\n[2/5] Creating model...")
    model = create_pitch_model()
    model.summary()
    
    # Compile model
    print("\n[3/5] Compiling model...")
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=args.learning_rate),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    # Callbacks
    callbacks = [
        keras.callbacks.ModelCheckpoint(
            filepath=os.path.join(args.checkpoint_dir, 'pitch_model_best.h5'),
            monitor='val_accuracy',
            save_best_only=True,
            verbose=1
        ),
        keras.callbacks.TensorBoard(
            log_dir=args.tensorboard_dir,
            histogram_freq=1
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
    model_path = Path(args.output_dir) / 'pitch_model.h5'
    model.save(model_path)
    print(f"  Model saved to: {model_path}")
    
    # Save training history
    history_path = Path(args.output_dir) / 'training_history.json'
    with open(history_path, 'w') as f:
        json.dump(history.history, f, indent=2)
    print(f"  History saved to: {history_path}")
    
    print("\n" + "=" * 60)
    print("Training completed successfully!")
    print("=" * 60)
    
    return model, history


def main():
    parser = argparse.ArgumentParser(description='Train pitch detection model')
    
    # Data arguments
    parser.add_argument('--data-dir', type=str, default='../data/pitch_dataset',
                       help='Path to dataset directory')
    
    # Training arguments
    parser.add_argument('--epochs', type=int, default=100,
                       help='Number of training epochs')
    parser.add_argument('--batch-size', type=int, default=32,
                       help='Batch size for training')
    parser.add_argument('--learning-rate', type=float, default=0.001,
                       help='Learning rate')
    
    # Augmentation
    parser.add_argument('--augmentation', action='store_true',
                       help='Enable data augmentation')
    
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
    print("1. Evaluate model: python ../utils/evaluation.py --model ../models/pitch_model.h5")
    print("2. Convert to TFLite: python ../utils/tflite_converter.py --model ../models/pitch_model.h5")
    print("3. Deploy to Android: cp ../models/pitch_model.tflite ../../app/src/main/assets/")


if __name__ == '__main__':
    main()
