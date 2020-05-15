/* 
 * Copyright (C) 2020 Ceridwen Limited
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.circulation.SIP.messages;

import com.circulation.SIP.annotations.Command;
import com.circulation.SIP.annotations.PositionedField;
import com.circulation.SIP.annotations.TaggedField;
import com.circulation.SIP.exceptions.*;
import com.circulation.SIP.fields.*;
import com.circulation.SIP.types.enumerations.AbstractEnumeration;
import com.circulation.SIP.types.flagfields.AbstractFlagField;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.PropertyDescriptor;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class Message implements Serializable {
    /**
   * 
   */
    private static final long serialVersionUID = 1609258005567594730L;
    public static final String PROP_CHARSET = "com.circulation.SIP.charset";
    public static final String PROP_AUTOPOPULATE = "com.circulation.SIP.messages.AutoPopulationEmptyRequiredFields";
    public static final String PROP_VARIABLE_FIELD_ORDERING = "com.ceridwen.circulation.SIP.VariableFieldOrdering";

    public static final String PROP_AUTOPOPULATE_OFF = "off";
    public static final String PROP_AUTOPOPULATE_DECODE = "decode";
    public static final String PROP_AUTOPOPULATE_ENCODE = "encode";
    public static final String PROP_AUTOPOPULATE_BIDIRECTIONAL = "bidirectional";
    
    public static final String PROP_AUTOPOPULATE_DEFAULT = PROP_AUTOPOPULATE_BIDIRECTIONAL;
    
    public static final String PROP_VARIABLE_FIELD_ORDERING_ALPHABETICAL = "alphabetical";
    public static final String PROP_VARIABLE_FIELD_ORDERING_SPECIFICATION = "specification";
 
    public static final String PROP_VARIABLE_FIELD_ORDERING_DEFAULT =  PROP_VARIABLE_FIELD_ORDERING_ALPHABETICAL;

    private static final String PROP_DEFAULT_CHARSET = "cp850";

    private static Log log = LogFactory.getLog(Message.class);

    private Character SequenceCharacter = null;

    public void setSequenceCharacter(Character sequenceCharacter) {
    	this.SequenceCharacter = sequenceCharacter;
    }

    public Character getSequenceCharacter() {
        return this.SequenceCharacter;
    }

    public static String getCharsetEncoding() {
        return System.getProperty(PROP_CHARSET, PROP_DEFAULT_CHARSET);      
    }
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    private String mangleDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd    HHmmss");
        return sdf.format(date);
    }

    private Date demangleDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd    HHmmss");
        try {
            return sdf.parse(date);
        } catch (Exception ex) {
            return null;
        }
    }

    private String[] getProp(PropertyDescriptor desc, FieldDefinition SIPField, boolean autoPop) throws MandatoryFieldOmitted {
        String[] ret = null;
        try {
            Method read = desc.getReadMethod();
            Object value = null;
            if (read != null) {
                value = read.invoke(this, new Object[0]);
            } else {
            if (desc.getPropertyType() == Boolean.class) {
                    read = this.getClass().getMethod("is" + desc.getName().substring(0, 1).toUpperCase() + desc.getName().substring(1), new Class[]{});
                    if (read != null) {
                        value = read.invoke(this, new Object[0]);                        
                    }
                }                
            }
            if (desc.getPropertyType() == Boolean.class) {
                if (value == null) {
                    if (SIPField != null) {
                        if (SIPField.policy != null) {
                            if (SIPField.policy == FieldPolicy.REQUIRED) {
                                if (desc.getName().equalsIgnoreCase("magneticMedia")) {
                                    ret = new String[] { "U" };
                                } else {
                                    if (!autoPop) {
                                        throw new MandatoryFieldOmitted(desc.getDisplayName());
                                    }
                                    if (desc.getName().equalsIgnoreCase("ok")) {
                                        ret = new String[] { "0" };
                                    } else {
                                        ret = new String[] { "N" };
                                    }
                                }
                            }
                        }
                    }
                } else if (desc.getName().equalsIgnoreCase("ok")) {
                    ret = new String[] { ((Boolean) value).booleanValue() ? "1" : "0" };
                } else {
                    ret = new String[] { ((Boolean) value).booleanValue() ? "Y" : "N" };
                }
            } else if (desc.getPropertyType() == Date.class) {
                if (value != null) {
                    ret = new String[] { this.mangleDate((Date) value) };
                } else {
                    if (SIPField != null) {
                        if (SIPField.policy != null) {
                            if (SIPField.policy == FieldPolicy.REQUIRED) {
                                if (!autoPop) {
                                    throw new MandatoryFieldOmitted(desc.getDisplayName());
                                }
                                ret = new String[] { this.mangleDate(new Date()) };
                            }
                        }
                    }
                }
            } else if (desc.getPropertyType() == String[].class) {
                if (value != null) {
                    ret = (String[]) value;
                }
            } else if (desc.getPropertyType() == Integer.class) {
                if (value != null) {
                    if (SIPField.length != 0) {
                        ret = new String[] { String.format("%0" + SIPField.length + "d", value) };
                    } else {
                        ret = new String[] { value.toString() };
                    }
                } else {
                    if (SIPField != null) {
                        if (SIPField.policy != null) {
                            if (SIPField.policy == FieldPolicy.REQUIRED) {
                                if (!autoPop) {
                                    throw new MandatoryFieldOmitted(desc.getDisplayName());
                                }
                                if (SIPField.length != 0) {
                                    ret = new String[] { String.format("%0" + SIPField.length + "d", 0) };
                                } else {
                                    ret = new String[] { "0" };
                                }
                            }
                        }
                    }
                }
            } else {
                if (value != null) {
                    ret = new String[] { value.toString() };
                } else {
                    if (SIPField != null) {
                        if (SIPField.policy != null) {
                            if (SIPField.policy == FieldPolicy.REQUIRED) {
                                if (!autoPop) {
                                    throw new MandatoryFieldOmitted(desc.getDisplayName());
                                }
                                Class<?>[] interfaces = desc.getPropertyType().getInterfaces();
                                for (Class<?> interfce : interfaces) {
                                    if (interfce == AbstractEnumeration.class) {
                                        Method mthd = desc.getPropertyType().getDeclaredMethod("values",
                                                new Class[] {});
                                        Object[] values = (Object[]) mthd.invoke(null, new Object[] {});
                                        if (values.length > 0) {
                                            ret = new String[] { values[0].toString() };
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (MandatoryFieldOmitted mfo) {
            throw mfo;
        } catch (Exception ex) {
            Message.log.error("Unexpected error getting " + desc.getDisplayName(), ex);
        }

        return (ret != null) ? ret : new String[] { "" };
    }

    private String pad(String input, PositionedFieldDefinition field) {
        StringBuffer ret = new StringBuffer();

        ret.append(input);

        while (ret.length() <= (field.end - field.start)) {
            ret.append(" ");
        }

        return ret.toString();
    }

    public String encode() throws MandatoryFieldOmitted, InvalidFieldLength, MessageNotUnderstood {
      return this.encode(this.getSequenceCharacter());
    }
    
    public String encode(boolean autoPop) throws MandatoryFieldOmitted, InvalidFieldLength, MessageNotUnderstood {
      return this.encode(this.getSequenceCharacter(), autoPop);
    }    
    
    public String encode(Character sequence) throws MandatoryFieldOmitted, InvalidFieldLength, MessageNotUnderstood {
        String pop = System.getProperty(Message.PROP_AUTOPOPULATE, PROP_AUTOPOPULATE_BIDIRECTIONAL);
        boolean autoPop = false;
        if (pop.equalsIgnoreCase(PROP_AUTOPOPULATE_ENCODE) || pop.equalsIgnoreCase(PROP_AUTOPOPULATE_DEFAULT)) {
            autoPop = true;
        }
        return encode(sequence, autoPop);
    }

    private String encode(Character sequence, boolean autoPop) throws MandatoryFieldOmitted, InvalidFieldLength, MessageNotUnderstood {
        Map<Integer, String> fixed = new TreeMap<Integer, String>();
        String order = System.getProperty(Message.PROP_VARIABLE_FIELD_ORDERING, PROP_VARIABLE_FIELD_ORDERING_DEFAULT);
        Map<String, String[]> variable;
        if (order.equalsIgnoreCase(Message.PROP_VARIABLE_FIELD_ORDERING_SPECIFICATION)) {
          variable = new LinkedHashMap<String, String[]>();
        } else {
          variable = new TreeMap<String, String[]>();         
        }
        StringBuffer message = new StringBuffer();

        Field[] fields = this.getClass().getDeclaredFields(); 

        for (Field fld : fields) {
          if (fld.isAnnotationPresent(PositionedField.class)) {
            PositionedField annotation = (PositionedField)fld.getAnnotation(PositionedField.class);               
            PositionedFieldDefinition field = Fields.getPositionedFieldDefinition(this.getClass().getName(), fld.getName(), annotation);
            PropertyDescriptor desc;
            try {
              desc = PropertyUtils.getPropertyDescriptor(this, fld.getName());
            } catch (Exception ex) {
              throw new java.lang.AssertionError("Introspection problem during encoding for " + fld.getName() + " in " + this.getClass().getName());
            }
            if (desc == null) {
              throw new java.lang.AssertionError("Introspection problem during encoding for " + fld.getName() + " in " + this.getClass().getName());                    
            }
            String[] value = this.getProp(desc, field, autoPop);
//                    if (StringUtils.isEmpty(value[0])) {
//                      if (field.policy == FieldPolicy.REQUIRED) {
//                            throw new MandatoryFieldOmitted(desc.getDisplayName());
//                        }
//                    }
            if (value[0].length() > (field.end - field.start + 1)) {
              throw new InvalidFieldLength(desc.getDisplayName(), (field.end - field.start + 1));
            }
            if ((desc.getPropertyType() == Date.class) || (desc.getPropertyType() == Boolean.class) || (desc.getPropertyType() == Integer.class)) {
              if (!(StringUtils.isEmpty(value[0]) || (value[0].length() == (field.end - field.start + 1)))) {
                throw new java.lang.AssertionError("FixedFieldDescriptor for " + desc.getDisplayName() + " in " + this.getClass().getSimpleName()
                    + ", start/end (" + field.start + "," + field.end + ") invalid for type " +
                    desc.getPropertyType().getName());
              }
            }
            if (fixed.containsKey(Integer.valueOf(field.start))) {
              throw new java.lang.AssertionError("Positioning error inserting field at " + field.start + " for class " + this.getClass().getName());                    
            }
            fixed.put(new Integer(field.start), this.pad(value[0], field));
          }
          if (fld.isAnnotationPresent(TaggedField.class)) {
            TaggedField annotation = (TaggedField)fld.getAnnotation(TaggedField.class);               
            TaggedFieldDefinition field = Fields.getTaggedFieldDefinition(this.getClass().getName(), fld.getName(), annotation);
            PropertyDescriptor desc;
            try {
              desc = PropertyUtils.getPropertyDescriptor(this, fld.getName());
            } catch (Exception ex) {
              throw new java.lang.AssertionError("Introspection problem during encoding for " + fld.getName() + " in " + this.getClass().getName());
            }
            if (desc == null) {
              throw new java.lang.AssertionError("Introspection problem during encoding for " + fld.getName() + " in " + this.getClass().getName());                    
            }
            String[] value = this.getProp(desc, field, autoPop);
            if (value.length > 0 && StringUtils.isNotEmpty(value[0])) {
              if (field.length != 0) {
                if (desc.getPropertyType() == String.class) {
                  if (value[0].length() > field.length) {
                    throw new InvalidFieldLength(desc.getDisplayName(), field.length);
                  }
                } else {
                  if (value[0].length() != field.length) {
                    throw new InvalidFieldLength(desc.getDisplayName(), field.length);
                  }
                }
              }
              variable.put(field.tag, value);
            } else if (field.policy == FieldPolicy.REQUIRED) {
              variable.put(field.tag, new String[]{""});
            }
          }
        }

        if (this.getClass().isAnnotationPresent(Command.class)) {
            message.append(((Command)(this.getClass().getAnnotation(Command.class))).value());
        } else {
            throw new java.lang.AssertionError("No command annotation present for class " + this.getClass().getName());
        }

        Iterator<Integer> fixedIterate = fixed.keySet().iterator();
        while (fixedIterate.hasNext()) {
            Integer key = fixedIterate.next();
            if (message.length() != key.intValue()) {
                throw new java.lang.AssertionError("Positioning error inserting field at " + key + " for class " + this.getClass().getName());
            }            
            message.append(fixed.get(key));
        }

        Iterator<String> varIterate = variable.keySet().iterator();
        while (varIterate.hasNext()) {
            String key = varIterate.next();
            String[] values = variable.get(key);
            for (String value : values) {
                message.append(key);
                message.append(value);
                message.append(TaggedFieldDefinition.TERMINATOR);
            }
        }

        return this.addChecksum(message.toString(), sequence);
    }

    private void setProp(PropertyDescriptor desc, String value) {
        try {
            if (desc.getPropertyType() == Boolean.class) {
                desc.getWriteMethod().invoke(this,
                                     new Object[] { value.equalsIgnoreCase("U") ? null :
                                             new Boolean(value.
                                                     equalsIgnoreCase("Y") ||
                                                     value.equalsIgnoreCase("1")) });
                return;
            }
            if (desc.getPropertyType() == Date.class) {
                desc.getWriteMethod().invoke(this,
                                     new Object[] { this.demangleDate(value) });
                return;
            }
            if (desc.getPropertyType() == Integer.class) {
                    if (!value.trim().isEmpty()) {
                desc.getWriteMethod().invoke(this,
                                     new Object[] {Integer.valueOf(value.trim()) });
                    }
                return;
            }
            if (desc.getPropertyType() == String.class) {
                desc.getWriteMethod().invoke(this,
                                     new Object[] { new String(value) });
                return;
            }
            if (desc.getPropertyType().getSuperclass() == AbstractFlagField.class) {
                Object data = desc.getPropertyType().getConstructor(new Class[] { String.class }).newInstance(new Object[] { new String(value) });
                    if (data != null) {
                desc.getWriteMethod().invoke(this,
                        new Object[] { data });
                    }
                return;
            }
            Class<?>[] interfaces = desc.getPropertyType().getInterfaces();
            for (Class<?> interfce : interfaces) {
                if (interfce == AbstractEnumeration.class) {
                    Method mthd = interfce.getDeclaredMethod("getKey",
                            new Class[] { String.class });
                    Method mthdInst = desc.getPropertyType().getDeclaredMethod("values",
                            new Class[] {});
                    Object[] values = (Object[]) mthdInst.invoke(null, new Object[] {});
                    if (values.length > 0) {
                        Object data = mthd.invoke(values[0],
                                new Object[] { new String(value) });
                        desc.getWriteMethod().invoke(this,
                                new Object[] { data });
                        return;
                    }
                }
            }
            if (desc.getPropertyType() == String[].class) {
                String[] current = (String[]) desc.getReadMethod().invoke(this, new Object[0]);
                if (current == null) {
                    desc.getWriteMethod().invoke(this,
                            new Object[] { new String[] { new String(value) } });
                    return;
                } else {
                    List<String> l = new ArrayList<String>(current.length + 1);
                    l.addAll(Arrays.asList(current));
                    l.add(new String(value));
                    desc.getWriteMethod().invoke(this,
                            new Object[] { l.toArray(new String[l.size()]) });
                    return;
                }
            }
        } catch (Exception ex) {
            Message.log.error("Unexpected error setting " + desc.getDisplayName() + " to " + value, ex);
        }
    }

    public static Message decode(String message, Character sequence, boolean checksumCheck) throws MandatoryFieldOmitted, ChecksumError, SequenceError,
        MessageNotUnderstood {
        String pop = System.getProperty(Message.PROP_AUTOPOPULATE, PROP_AUTOPOPULATE_BIDIRECTIONAL);
        boolean autoPop = false;
        if (pop.equalsIgnoreCase(PROP_AUTOPOPULATE_DECODE) || pop.equalsIgnoreCase(PROP_AUTOPOPULATE_DEFAULT)) {
            autoPop = true;
        }
      return decode(message, sequence, checksumCheck, autoPop);
    }
    
    private static Message decode(String message, Character sequence, boolean checksumCheck, boolean autoPop) throws MandatoryFieldOmitted, ChecksumError, SequenceError,
            MessageNotUnderstood {
        if (checksumCheck) {
            if (!Message.CheckChecksum(message)) {
                throw new ChecksumError();
            }
        }
        Character sequenceCharacter = Message.GetSequenceCharacter(message);

        if (sequence != null) {
            if (sequenceCharacter != null) {
                if (!sequence.equals(sequenceCharacter)) {
                    throw new SequenceError();
                }
            }
        }

        if (message == null) {
            throw new MessageNotUnderstood();
        }
        if (message.length() < 2) {
            throw new MessageNotUnderstood();
        }
        String command = message.substring(0, 2);
        Class<? extends Message> msgClass = Message.messages.get(command);
        if (msgClass == null) {
            throw new MessageNotUnderstood();                
        }
        Message msg;
    try {
            msg = msgClass.newInstance();
        } catch (Exception ex) {
            throw new java.lang.AssertionError("Instantiation problem creating new " + msgClass.getName());
        }
        Field[] fields = msg.getClass().getDeclaredFields();

        int fixedFieldEnd = 2;

        for (Field fld : fields) {
            if (fld.isAnnotationPresent(PositionedField.class)) {
                PositionedField annotation = fld.getAnnotation(PositionedField.class);
                PositionedFieldDefinition field = Fields.getPositionedFieldDefinition(msg.getClass().getName(), fld.getName(), annotation);
                PropertyDescriptor desc;
                try {
                    desc = PropertyUtils.getPropertyDescriptor(msg, fld.getName());
                } catch (Exception ex) {
                    throw new java.lang.AssertionError("Introspection problem during decoding for " + fld.getName() + " in " + msg.getClass().getName());
                }
                if (desc == null) {
                    throw new java.lang.AssertionError("Introspection problem during decoding for " + fld.getName() + " in " + msg.getClass().getName());                    
                }
                String value = "";
                if (message.length() > field.end) {
                  value = message.substring(field.start, field.end + 1);
                } else {
                  if (!autoPop) {
                    throw new MandatoryFieldOmitted(desc.getDisplayName());                   
                  }
                }
                msg.setProp(desc, value);
                if (fixedFieldEnd < field.end) {
                    fixedFieldEnd = field.end;
                }
            }
        }

        msg.parseVarFields(fixedFieldEnd + 1, message);
        
        msg.SequenceCharacter = sequenceCharacter;

        for (Field fld : fields) {
            if (fld.isAnnotationPresent(TaggedField.class)) {
              TaggedField annotation = fld.getAnnotation(TaggedField.class);
              TaggedFieldDefinition field = Fields.getTaggedFieldDefinition(msg.getClass().getName(), fld.getName(), annotation);
                PropertyDescriptor desc;
                try {
                    desc = PropertyUtils.getPropertyDescriptor(msg, fld.getName());
                } catch (Exception ex) {
                    throw new java.lang.AssertionError("Introspection problem during decoding for " + fld.getName() + " in " + msg.getClass().getName());
                }
                if (desc == null) {
                    throw new java.lang.AssertionError("Introspection problem during decoding for " + fld.getName() + " in " + msg.getClass().getName());                    
                }
                try {
                  msg.getProp(desc, field, false);
                } catch (MandatoryFieldOmitted ex) {
              if (autoPop) {
                msg.setProp(desc, "");
              } else {
                throw ex;
              }
                }
            }
        }

        return msg;
    }

    private static boolean CheckChecksum(String message) {
        try {
            String tail = message.substring(message.length() - 6);
            if (!tail.startsWith("AZ")) {
                return true;
            }
            String truncated = message.substring(0, message.length() - 4);
            String check = tail.substring(2);
            String checksum = Message.calculateChecksum(truncated);
            return (checksum.equals(check));
        } catch (Exception ex) {
        }

        return true;
    }

    private static Character GetSequenceCharacter(String message) {
        try {
            String tail = message.substring(message.length() - 9);
            if (!tail.startsWith("AY")) {
                return null;
            }
            return tail.charAt(2);
        } catch (Exception ex) {
        }

        return null;
    }

    protected static String calculateChecksum(String data) throws UnsupportedEncodingException {
        int checksum = 0;
        // Fix from Rustam Usmanov
        byte[] bytes = data.getBytes(Message.getCharsetEncoding());
        for (byte b : bytes) {          
          // Fix from Rustam Usmanov
          checksum += b & 0xff;
        }
        checksum = -checksum & 0xffff;
        // Fix from Rustam Usmanov
        return String.format("%1$04X", checksum);
    }

    protected String addChecksum(String command, Character sequence) {
        StringBuffer check = new StringBuffer();
        if (sequence != null) {
            check.append("AY");
            check.append(sequence);
            check.append("AZ");
            try {
                check.append(Message.calculateChecksum(command + check.toString()));
                return command + check.toString();
            } catch (Exception e) {
                return command;
            }
        } else {
            return command;
        }
    }

    private void parseVarFields(int offset, String data) {
        int status = 1;
        StringBuffer fieldtag = new StringBuffer();
        StringBuffer fielddata = new StringBuffer();

        for (int n = offset; n < data.length(); n++) {
            if (status == 1) {
                fieldtag = new StringBuffer();
                fieldtag.append(data.charAt(n));
                status = 2;
            } else if (status == 2) {
                fielddata = new StringBuffer();
                fieldtag.append(data.charAt(n));
                status = 3;
            } else if (status == 3) {
                if (data.charAt(n) == TaggedFieldDefinition.TERMINATOR) {
                    this.setFieldProp(fieldtag.toString(), fielddata.toString());
                    status = 1;
                } else {
                    fielddata.append(data.charAt(n));
                }
            }
        }
        return;
    }

    private void setFieldProp(String tag, String data) {
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field fld : fields) {
                if (fld.isAnnotationPresent(TaggedField.class)) {
                    TaggedField annotation = fld.getAnnotation(TaggedField.class);
                    TaggedFieldDefinition field = Fields.getTaggedFieldDefinition(this.getClass().getName(), fld.getName(), annotation);
                    PropertyDescriptor desc;
                    try {
                        desc = PropertyUtils.getPropertyDescriptor(this, fld.getName());
                    } catch (Exception ex) {
                        throw new java.lang.AssertionError("Introspection problem during decoding for " + fld.getName() + " in " + this.getClass().getName());                                            
                    }
                    if (desc == null) {
                        throw new java.lang.AssertionError("Introspection problem during decoding for " + fld.getName() + " in " + this.getClass().getName());                    
                    }
                    if (field.tag.equals(tag)) {
                        this.setProp(desc, data);
                    }
                }
            }
        }

    public void xmlEncode(OutputStream strm) {
        XMLEncoder out = new XMLEncoder(strm);
        out.writeObject(this);
        out.flush();
    }

    public static Message xmlDecode(InputStream strm) {
        XMLDecoder in = new XMLDecoder(strm);
        Message msg = (Message) in.readObject();
        return msg; 
    }

    private static Hashtable<String, Class<? extends Message>> messages = new Hashtable<String, Class<? extends Message>>();

    static {
        for (Messages m: Messages.values()) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Message> message = (Class<? extends Message>)Class.forName(Messages.class.getPackage().getName() +  "." + m.name());
                if (message != null) {
                    if (message.isAnnotationPresent(Command.class)) {
                        String cmd = ((Command)message.getAnnotation(Command.class)).value();
                        if (cmd.isEmpty()) {
                            throw new java.lang.AssertionError(m.name() + " has empty command string.");                                        
                        }
                        if (Message.messages.containsKey(cmd)) {
                            throw new java.lang.AssertionError(m.name() + " duplicates command string.");                                                                    
                        }
                        Message.messages.put(cmd, (Class<? extends Message>)message);
                    }
                }
            } catch (Exception ex) {
                Message.log.warn(m.name() + " not yet implemented.");
            }
        }
    }

    @Override
    public String toString() {
        ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        this.xmlEncode(buffer);
        return new String(buffer.toByteArray());
    }

}