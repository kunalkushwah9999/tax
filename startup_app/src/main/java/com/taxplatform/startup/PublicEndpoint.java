package com.taxplatform.startup;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Explicit trusted reverse-proxy origin. Request forwarding headers cannot select it. */
public record PublicEndpoint(String origin,String authority) {
    public static PublicEndpoint https(String text){
        try{
            URI value=new URI(text);String host=value.getHost();int port=value.getPort();
            if(!"https".equals(value.getScheme())||host==null||!host.matches("[A-Za-z0-9][A-Za-z0-9.-]{0,252}")||value.getUserInfo()!=null||value.getRawQuery()!=null||value.getRawFragment()!=null||!(value.getRawPath().isEmpty()||value.getRawPath().equals("/"))||port==0||port>65535)throw new IllegalArgumentException("explicit HTTPS origin required");
            String authority=host.toLowerCase(Locale.ROOT)+(port<0||port==443?"":":"+port);return new PublicEndpoint("https://"+authority,authority);
        }catch(URISyntaxException e){throw new IllegalArgumentException("invalid HTTPS origin");}
    }
}
