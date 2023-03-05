package model.game;

class ChestRequestHistory {

    private int elegant;
    private int jewel;
    private int royal;

    public void add(int elegant, int jewel, int royal) {
        this.elegant += elegant;
        this.jewel += jewel;
        this.royal += royal;
    }

    public boolean isAllowed(ChestRequest request) {
        if (request.getElegant() + this.elegant > 50) {
            return false;
        }
        if (request.getJewel() + this.jewel > 25) {
            return false;
        }
        return request.getRoyal() + this.royal <= 10;
    }

    public void max() {
        this.elegant = 50;
        this.jewel = 25;
        this.royal = 10;
    }

    public int getElegant() {
        return elegant;
    }

    public int getJewel() {
        return jewel;
    }

    public int getRoyal() {
        return royal;
    }
}
