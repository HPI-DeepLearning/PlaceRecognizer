package de.hpi.placerecognizer;

class Classification {
    private int _id;
    private String _label;
    private float _probability;

    Classification(int id, String label, float probability) {
        _id = id; _label = label; _probability = probability;
    }

    public int get_id() { return _id; }
    String get_label() { return _label; }
    float get_probability() { return _probability; }
}
