# GMM Raga Classification

## Overview

The GMM (Gaussian Mixture Models) raga classifier uses statistical modeling to identify ragas based on pitch sequences. Unlike the neural network models, this is implemented directly in Kotlin and doesn't require TFLite.

## Model Architecture

**Type**: Gaussian Mixture Models (GMM)

**Input**: 
- Pitch sequence (list of frequencies in Hz)
- Tonic frequency (reference pitch)

**Output**:
- Raga probabilities for 10 supported ragas
- Top-3 raga candidates with confidence scores

**Supported Ragas**:
1. Yaman
2. Bhairav
3. Todi
4. Malkauns
5. Bhupali
6. Desh
7. Kafi
8. Bihag
9. Bageshree
10. Puriya Dhanashree

## How It Works

### 1. Feature Extraction

From a pitch sequence, extract:
- **Swar distribution**: Histogram of swars used
- **Interval patterns**: Common melodic intervals
- **Aroha/Avaroha**: Ascending/descending patterns
- **Pakad**: Characteristic phrases

### 2. GMM Training

For each raga, train a GMM on the feature vectors:
- Number of components: 3-5 per raga
- Covariance type: Full
- Initialization: K-means++

### 3. Classification

Given a new pitch sequence:
1. Extract features
2. Calculate log-likelihood for each raga's GMM
3. Apply softmax to get probabilities
4. Return top-3 candidates

## Implementation

The GMM classifier is already implemented in Kotlin:

**Location**: `app/src/main/java/com/example/riwaz/ml/GMMRagaClassifier.kt`

**Key Methods**:
```kotlin
fun classifyRaga(
    pitches: List<Float>,
    tonicFrequency: Float
): ClassificationResult

fun validateRagaCompliance(
    ragaName: String,
    pitches: List<Float>,
    tonicFrequency: Float
): Float
```

## Training (Python)

### Data Preparation

Collect raga recordings:
```
data/raga_dataset/
├── Yaman/
│   ├── recording_001.wav
│   ├── recording_002.wav
│   └── ...
├── Bhairav/
├── Todi/
└── ... (10 folders)
```

Requirements:
- 100+ recordings per raga
- 30-60 seconds each
- Clean, professional quality

### Feature Extraction

```python
import librosa
import numpy as np

def extract_raga_features(audio_file, tonic_freq):
    """Extract features for GMM training"""
    # Load audio
    y, sr = librosa.load(audio_file, sr=44100)
    
    # Extract pitch using pYIN
    f0, voiced_flag, voiced_probs = librosa.pyin(
        y, fmin=50, fmax=500, sr=sr
    )
    
    # Convert to swar sequence
    swars = pitch_to_swar_sequence(f0, tonic_freq)
    
    # Extract features
    features = {
        'swar_histogram': compute_swar_histogram(swars),
        'interval_distribution': compute_intervals(f0),
        'pitch_range': (np.min(f0), np.max(f0)),
        'mean_pitch': np.mean(f0),
        'std_pitch': np.std(f0)
    }
    
    return features
```

### Training Script

```python
from sklearn.mixture import GaussianMixture
import pickle

def train_gmm_classifier(data_dir, output_path):
    """Train GMM for each raga"""
    
    ragas = ['Yaman', 'Bhairav', 'Todi', ...]
    gmm_models = {}
    
    for raga in ragas:
        print(f"Training GMM for {raga}...")
        
        # Load features for this raga
        features = load_raga_features(data_dir, raga)
        
        # Train GMM
        gmm = GaussianMixture(
            n_components=5,
            covariance_type='full',
            max_iter=100,
            random_state=42
        )
        gmm.fit(features)
        
        gmm_models[raga] = gmm
    
    # Save models
    with open(output_path, 'wb') as f:
        pickle.dump(gmm_models, f)
    
    print(f"GMM models saved to {output_path}")
```

### Convert to Kotlin

The trained GMM parameters need to be converted to Kotlin:

```python
def export_gmm_to_kotlin(gmm_models, output_file):
    """Export GMM parameters as Kotlin code"""
    
    with open(output_file, 'w') as f:
        f.write("// Auto-generated GMM parameters\n\n")
        
        for raga, gmm in gmm_models.items():
            f.write(f"// {raga}\n")
            f.write(f"val {raga}_means = arrayOf(\n")
            for mean in gmm.means_:
                f.write(f"    floatArrayOf({', '.join(map(str, mean))}),\n")
            f.write(")\n\n")
            
            f.write(f"val {raga}_covariances = arrayOf(\n")
            for cov in gmm.covariances_:
                # Flatten covariance matrix
                f.write(f"    // Covariance matrix\n")
            f.write(")\n\n")
```

## Evaluation

### Metrics

- **Top-1 Accuracy**: > 80%
- **Top-3 Accuracy**: > 95%
- **Confusion Analysis**: Identify similar ragas

### Evaluation Script

```bash
python train_gmm_classifier.py \
    --data-dir ../data/raga_dataset \
    --output ../models/gmm_ragas.pkl \
    --evaluate
```

## Advantages of GMM

1. **No TFLite needed**: Pure Kotlin implementation
2. **Interpretable**: Can explain why a raga was chosen
3. **Small model size**: Just parameters, no neural network
4. **Fast inference**: Simple probability calculations
5. **Musicologically sound**: Based on raga theory

## Limitations

1. Requires good pitch detection
2. Needs sufficient pitch sequence (30+ notes)
3. May confuse similar ragas (e.g., Yaman vs Bihag)
4. Doesn't capture temporal dynamics well

## Integration

The GMM classifier is already integrated in `MLModelManager`:

```kotlin
// In MLModelManager.kt
ragaClassifier = GMMRagaClassifier()
isRagaModelAvailable = true
```

No additional deployment needed - it's part of the app code!

## Future Improvements

- [ ] Add more ragas (expand to 20+)
- [ ] Incorporate temporal features (HMM)
- [ ] Use deep learning for feature extraction
- [ ] Add raga similarity scores
- [ ] Support raga variations (different gharanas)
