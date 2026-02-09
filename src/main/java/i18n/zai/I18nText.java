package i18n.zai;

public class I18nText {
    private final String ownerFqn;
    private final String chinese;


    public I18nText(String ownerFqn, String chinese) {
        this.ownerFqn = ownerFqn;
        this.chinese = chinese;
    }


    public String getOwnerFqn() {
        return ownerFqn;
    }

    public String getChinese() {
        return chinese;
    }

}
