//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.03.27 at 12:34:04 PM PDT 
//


package edu.usc.ict.perception.pml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}faceGestureType"/>
 *         &lt;element name="gestureBool" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element ref="{}gestureValue" minOccurs="0"/>
 *         &lt;element ref="{}confidence" minOccurs="0"/>
 *         &lt;element ref="{}conf_str" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "faceGestureType",
    "gestureBool",
    "gestureValue",
    "confidence",
    "confStr"
})
@XmlRootElement(name = "faceGesture")
public class FaceGesture {

    @XmlElement(required = true)
    protected String faceGestureType;
    protected Boolean gestureBool;
    protected Double gestureValue;
    protected Double confidence;
    @XmlElement(name = "conf_str")
    protected String confStr;

    /**
     * Gets the value of the faceGestureType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFaceGestureType() {
        return faceGestureType;
    }

    /**
     * Sets the value of the faceGestureType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFaceGestureType(String value) {
        this.faceGestureType = value;
    }

    /**
     * Gets the value of the gestureBool property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isGestureBool() {
        return gestureBool;
    }

    /**
     * Sets the value of the gestureBool property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setGestureBool(Boolean value) {
        this.gestureBool = value;
    }

    /**
     * Gets the value of the gestureValue property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getGestureValue() {
        return gestureValue;
    }

    /**
     * Sets the value of the gestureValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setGestureValue(Double value) {
        this.gestureValue = value;
    }

    /**
     * Gets the value of the confidence property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getConfidence() {
        return confidence;
    }

    /**
     * Sets the value of the confidence property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setConfidence(Double value) {
        this.confidence = value;
    }

    /**
     * Gets the value of the confStr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConfStr() {
        return confStr;
    }

    /**
     * Sets the value of the confStr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConfStr(String value) {
        this.confStr = value;
    }

}
