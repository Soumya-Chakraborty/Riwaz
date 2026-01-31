# Enhanced Swar Precision Visualization Component

## Overview
The Enhanced Swar Precision Visualization component provides a comprehensive visual representation of pitch accuracy over time for Indian classical music practice sessions. It implements all the requirements specified in the design brief.

## Features

### 1. Scrollable Graph
- Interactive timeline visualization showing pitch accuracy over time
- Zoom and pan functionality for detailed analysis of longer recordings
- Dual-line visualization showing both accuracy and stability metrics

### 2. Visual Representation of Each Swar
- Individual swar markers with size indicating importance
- Color-coded accuracy indicators (green for accurate, yellow for moderate, red for inaccurate)
- Inner circles representing stability metrics

### 3. Color-Coded Feedback
- Green: High accuracy (>90%)
- Yellow: Moderate accuracy (70-90%)
- Red: Low accuracy (<70%)
- Cyan outline: Raga-specific notes
- Magenta outline: Forbidden notes

### 4. Timeline Visualization
- Continuous line showing accuracy trends throughout the recording
- Secondary line showing stability trends
- Grid overlay for easier interpretation

### 5. Interactive Elements
- Tap on individual swars for detailed feedback
- Zoom in/out for closer inspection
- Pan across longer recordings
- Expandable detailed view for comprehensive analysis

### 6. Smooth Animations
- Animated transitions between states
- Smooth zoom and pan interactions
- Animated progress indicators

### 7. Scalability
- Efficient rendering for longer recordings
- Optimized drawing algorithms
- Lazy loading for detailed views

### 8. Raga-Specific Indicators
- Visual distinction for raga-specific notes
- Highlighting of forbidden notes
- Integration with raga grammar validation

## Implementation Details

### Components Structure
```
AdvancedSwarPrecisionVisualization
├── TimelineVisualization (Canvas-based)
├── SwarDetailsCard (Detailed information)
├── SwarPrecisionLegend (Color key)
├── DetailedSwarTimeline (Expanded view)
└── TimelinePoint (Data model)
```

### Data Model
The component uses the `SwarData` model from the existing codebase:
- `name`: Name of the swar (Sa, Re, Ga, etc.)
- `accuracy`: Accuracy percentage (0.0-1.0)
- `stability`: Stability metric (0.0-1.0)
- `expectedFreq`: Expected frequency in Hz
- `detectedFreq`: Detected frequency in Hz
- `isMistake`: Boolean indicating if the note was played incorrectly

### Integration Points
- Works with existing `AnalysisData` structure
- Compatible with `RagaRegistry.RagaData` for raga-specific information
- Callback mechanism for swar selection (`onSwarSelected`)

## Usage

### Basic Implementation
```kotlin
AdvancedSwarPrecisionVisualization(
    swarData = analysisData.swarStats,
    ragaInfo = analysisData.ragaInfo,
    onSwarSelected = { swarData ->
        // Handle swar selection for detailed feedback
        println("Selected swar: ${swarData.name} with accuracy: ${swarData.accuracy}")
    }
)
```

### Customization Options
- `modifier`: Customize the layout properties
- `onSwarSelected`: Callback for when a swar is tapped
- Responsive to theme colors (uses iOSBlack, iOSRed, iOSGreen)

## Technical Specifications

### Canvas Operations
- Grid drawing with configurable lines
- Path-based accuracy and stability lines
- Circle-based swar markers with multiple layers
- Text overlays for swar labels

### Performance Considerations
- Efficient drawing algorithms for smooth scrolling
- Conditional rendering based on visibility
- Memory-efficient data structures

### Accessibility
- Sufficient contrast ratios
- Clear visual hierarchy
- Interactive elements with appropriate sizing

## Testing

The component includes a preview function for testing:
```kotlin
@Preview(showBackground = true)
@Composable
fun AdvancedSwarPrecisionVisualizationPreview()
```

Sample data generation utilities are available in `SwarPrecisionGenerator`.

## Dependencies

- Jetpack Compose Foundation
- Canvas API for custom drawing
- Material Design 3 components
- Existing Riwaz data models and utilities

## Future Enhancements

Potential areas for future development:
- Audio waveform overlay
- Export functionality for analysis reports
- Comparison view for multiple sessions
- Machine learning-based pattern recognition
- Integration with practice recommendations