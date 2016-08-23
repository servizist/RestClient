package it.sad.sii.network;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import java.lang.reflect.Type;

/**
 * Created by ldematte on 2/17/15.
 * <p/>
 * A helper to serialize/deserialize Joda DateTime with GSON
 */

public final class DateTimeDeserializer implements JsonDeserializer<DateTime>, JsonSerializer<DateTime> {
    public static final Type DATE_TIME_TYPE = new TypeToken<DateTime>() {}.getType();

    static final org.joda.time.format.DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public DateTime deserialize(final JsonElement je, final Type type,
                                final JsonDeserializationContext jdc) throws JsonParseException {
        String dateAsString = je.getAsString();
        return dateAsString.length() == 0 ? null : DATE_TIME_FORMATTER.parseDateTime(dateAsString);
    }

    @Override
    public JsonElement serialize(final DateTime src, final Type typeOfSrc,
                                 final JsonSerializationContext context) {

        if (src == null)
            return JsonNull.INSTANCE;
        return new JsonPrimitive(DATE_TIME_FORMATTER.print(src));
    }
}