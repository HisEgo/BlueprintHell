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

    public boolean isValid() {
        return balancedPorts && compatibleShapes;
    }

    public boolean isBalancedPorts() {
        return balancedPorts;
    }

    public boolean isCompatibleShapes() {
        return compatibleShapes;
    }

    public int getTotalInputPorts() {
        return totalInputPorts;
    }

    public int getTotalOutputPorts() {
        return totalOutputPorts;
    }

    public Map<PortShape, Integer> getInputPortShapes() {
        return inputPortShapes;
    }

    public Map<PortShape, Integer> getOutputPortShapes() {
        return outputPortShapes;
    }

    public String getShapeIssues() {
        return shapeIssues;
    }

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


