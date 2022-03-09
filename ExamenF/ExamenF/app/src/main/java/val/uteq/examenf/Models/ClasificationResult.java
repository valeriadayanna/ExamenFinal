package val.uteq.examenf.Models;

public class ClasificationResult {
    private int identifier = -1;
    private String alpha2code = "";
    private String Country = "";
    private float precision = 0;

    public ClasificationResult() {
    }

    public ClasificationResult(int identifier, String alpha2code, String country, float precision) {
        this.identifier = identifier;
        this.alpha2code = alpha2code;
        Country = country;
        this.precision = precision;
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public String getAlpha2code() {
        return alpha2code;
    }

    public void setAlpha2code(String alpha2code) {
        this.alpha2code = alpha2code;
    }

    public String getCountry() {
        return Country;
    }

    public void setCountry(String country) {
        Country = country;
    }

    public float getPrecision() {
        return precision;
    }

    public void setPrecision(float precision) {
        this.precision = precision;
    }
}
