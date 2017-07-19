# micot-fragility
A repository and Maven project for MICOT applications built on the General Fragility Model (GFM) framework.

# Input Schema
```json
{
    "$schema": "http://json-schema.org/draft-04/schema#",
    "title": "General Fragility Model Definitions",
    "description": "A set of schema definitions for the General Fragility Model (GFM) framework.",
    "type": "object",
    "additionalProperties":false,
    "properties":
    {
        "height":
        {
            "type": "number",
            "description": "Height of the power pole in meters."
        },
        "baseDiameter":
        {
            "type": "number",
            "description": "Pole base diameter in meters."
        },
        "topDiameter":
        {
            "type": "number",
            "description": "Pole top diameter in meters."
        },
        "cableSpan":
        {
            "type": "number",
            "description": "Span between poles in meters."
        },
        "woodDensity":
        {
            "type": "number",
            "description": "Wood density of the pole in kg/m^3."
        },
        "meanPoleStrength":
        {
            "type": "number",
            "description": "Mean value of the pole strength in pascals."
        },
        "stdDevPoleStrength":
        {
            "type": "number",
            "description": "Standard deviation of the pole strength in pascals."
        },
        "powerCableDiameter":
        {
            "type": "number",
            "description": "Power cable diameter in meters."
        },
        "powerCableNumber":
        {
            "type": "number",
            "description": "Number of power cables."
        },
        "powerCableWireDensity":
        {
            "type": "number",
            "description": "Power cable wire density in kg/m^3."
        },
        "powerCircuitName":
        {
            "type": "string",
            "description": "Identifier for the circuit for lines on this pole."
        },
        "powerAttachmentHeight":
        {
            "type": "number",
            "description": "Power cable attachment height in meters."
        },
        "commCableNumber":
        {
            "type": "number",
            "description": "Number of comm cables."
        },
        "commCableDiameter":
        {
            "type": "number",
            "description": "Comm cable diameter in meters."
        },
        "commCableWireDensity":
        {
            "type": "number",
            "description": "Comm cable wire density in kg/m^3."
        },
        "commAttachmentHeight":
        {
            "type": "number",
            "description": "Comm cable attachment height in meters."
        }
    }
}

```
