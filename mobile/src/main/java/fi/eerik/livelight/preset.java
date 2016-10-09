package fi.eerik.livelight;

/**
 * Created by Henri Rantanen on 16.6.2015.
 */
public class preset {

    public static preset[] presets = {
            new preset(0, "Automatic Light Control", "", "Setting automatic light control..."),
            new preset(1, "Turn lights off", "", "Turning lights off..."),
            //new preset(2, "Daylight setting", "color1=6a8171&color2=2b0f00&color3=4789ff&color4=b0ff00&color5=97ffff&color6=910000&color7=ffbb9a&color8=ff0000", "Setting bright daylight..."),
            new preset(2, "Daylight setting", "preset=3", "Setting bright daylight..."),
            new preset(3, "Evening setting", "preset=4", "Setting dim warm  lighting..."),
            new preset(4, "Night light", "preset=5", "Setting moody night lighting..."),
            new preset(5, "Bed Underglow", "preset=6", "Going to the toilet again?"),
            new preset(6, "Movie Mode", "preset=7", "Let there be blue!"),
    };

    private int id;
    private String name;
    private String link;
    private String text;

    public preset(){
        super();
    }

    public preset(int id, String name, String link, String text) {
        super();
        this.id = id;
        this.name = name;
        this.link = link;
        this.text = text;
    }

    @Override
    public String toString() {

        return this.name;
    }

    public String text() {

        return this.text;
    }

    public String getaddress() {
        if (this.id == 0) {
            return "api.php?mode=auto" + "&key=" + settings.apikey;
        }
        if (this.id == 1) {
            return "api.php?mode=off" + "&key=" + settings.apikey;
        }
        else
        {
            //return "lib/setAll.php?" + this.link + "&key=" + settings.apikey;
            //return "api.php?mode=setAll&" + this.link + "&key=" + settings.apikey;
            return "api.php?mode=preset&" + this.link + "&key=" + settings.apikey;
        }

    }
}