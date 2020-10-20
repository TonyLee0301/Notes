package info.tonylee.studio.spring.init.conversion;

import org.springframework.core.convert.converter.Converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class String2DateConverter implements Converter<String, Date> {

    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyyy-MM-dd HH:mm:ss");

    public Date convert(String s) {
        try {
            return sdf.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

}
