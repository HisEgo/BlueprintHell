package model;

import java.util.Map;

public class LevelValidationResult {
    private final boolean balancedPorts;
    private final boolean compatibleShapes;
    private final int totalInputPorts;
    private final int totalOutputPorts;
    private final Map<PortShape, Integer> inputPortShapes;
    private final Map<PortShape, Integer> outputPortShapes;
    private final String shapeIssues;

    public LevelValidationResult(boolean balancedPorts, boolean compatibleShapes,
                                 int totalInputPorts, int totalOutputPorts,
                                 Map<PortShape, Integer> inputPortShapes,
                                 Map<PortShape, Integer> outputPortShapes,
                                 String shapeIssues) {
        this.balancedPorts = balancedPorts;
        this.compatibleShapes = compatibleShapes;
        this.totalInputPorts = totalInputPorts;
        this.totalOutputPorts = totalOutputPorts;
        this.inputPortShapes = inputPortShapes;
        this.outputPortShapes = outputPortShapes;
        this.shapeIssues = shapeIssues;
    }

    /**
     * Checks if the level design is valid for complete port connectivity.
     */
    public boolean isValid() {
        return balancedPorts && compatibleShapes;
    }

    /**
     * Gets whether the total number of input and output ports are balanced.
     */
    public boolean isBalancedPorts() {
        return balancedPorts;
    }

    /**
     * Gets whether all port shapes have matching input/output counts.
     */
    public boolean isCompatibleShapes() {
        return compatibleShapes;
    }

    /**
     * Gets the total number of input ports.
     */
    public int getTotalInputPorts() {
        return totalInputPorts;
    }

    /**
     * Gets the total number of output ports.
     */
    public int getTotalOutputPorts() {
        return totalOutputPorts;
    }

    /**
     * Gets the count of input ports by shape.
     */
    public Map<PortShape, Integer> getInputPortShapes() {
        return inputPortShapes;
    }

    /**
     * Gets the count of output ports by shape.
     */
    public Map<PortShape, Integer> getOutputPortShapes() {
        return outputPortShapes;
    }

    /**
     * Gets a description of any shape compatibility issues.
     */
    public String getShapeIssues() {
        return shapeIssues;
    }

    /**
     * Gets a human-readable summary of the validation result.
     */
    public String getSummary() {
        if (isValid()) {
            return String.format("Level design is valid. Total ports: %d input, %d output",
                    totalInputPorts, totalOutputPorts);
        }

        StringBuilder summary = new StringBuilder("Level design has issues:");
        if (!balancedPorts) {
            summary.append(String.format(" Port count mismatch (%d input vs %d output)",
                    totalInputPorts, totalOutputPorts));
        }
        if (!compatibleShapes) {
            summary.append(" Shape compatibility issues:").append(shapeIssues);
        }
        return summary.toString();
    }
}

