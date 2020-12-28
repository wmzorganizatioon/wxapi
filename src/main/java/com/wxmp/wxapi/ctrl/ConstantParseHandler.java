package com.wxmp.wxapi.ctrl;

import com.wxmp.wxapi.vo.ComponentContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ConstantParseHandler extends DefaultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstantParseHandler.class);

    private String currentTag;  //记录解析的节点标签

    private ComponentContent componentContent;

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        currentTag = qName;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
        String value = new String(ch, start, length);
        if ("CreateTime".equals(currentTag)) {
            componentContent.setCreatTime(value);
        } else if ("InfoType".equals(currentTag)) {
            componentContent.setInfoType(value);
        } else if ("ComponentVerifyTicket".equals(currentTag)) {
            componentContent.setComponentVerifyTicket(value);
        } else if ("AppId".equals(currentTag)) {
            componentContent.setAppId(value);
        }
    }

    public ComponentContent getComponentContent() {
        return componentContent;
    }

    public void setComponentContent(ComponentContent componentContent) {
        this.componentContent = componentContent;
    }
}
