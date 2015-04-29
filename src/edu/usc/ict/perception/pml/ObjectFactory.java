//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.03.27 at 12:34:04 PM PDT 
//


package edu.usc.ict.perception.pml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the edu.usc.ict.perception.pml package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _PostureType_QNAME = new QName("", "postureType");
    private final static QName _BehaviorType_QNAME = new QName("", "behaviorType");
    private final static QName _PresenceType_QNAME = new QName("", "presenceType");
    private final static QName _ConfStr_QNAME = new QName("", "conf_str");
    private final static QName _PostureStatus_QNAME = new QName("", "postureStatus");
    private final static QName _PostureDegree_QNAME = new QName("", "postureDegree");
    private final static QName _HeadGestureType_QNAME = new QName("", "headGestureType");
    private final static QName _Confidence_QNAME = new QName("", "confidence");
    private final static QName _GestureValue_QNAME = new QName("", "gestureValue");
    private final static QName _GazeCategoryHorizontal_QNAME = new QName("", "gazeCategoryHorizontal");
    private final static QName _BehaviorValue_QNAME = new QName("", "behaviorValue");
    private final static QName _GazeCategoryDirection_QNAME = new QName("", "gazeCategoryDirection");
    private final static QName _FaceGestureType_QNAME = new QName("", "faceGestureType");
    private final static QName _HandGestureType_QNAME = new QName("", "handGestureType");
    private final static QName _GazeCategoryVertical_QNAME = new QName("", "gazeCategoryVertical");
    private final static QName _ArmType_QNAME = new QName("", "armType");
    private final static QName _PoseType_QNAME = new QName("", "poseType");
    private final static QName _SpeechRateValue_QNAME = new QName("", "speechRateValue");
    private final static QName _SpeechVariationValue_QNAME = new QName("", "speechVariationValue");
    private final static QName _SpeechFractionValue_QNAME = new QName("", "speechFractionValue");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: edu.usc.ict.perception.pml
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Activity }
     * 
     */
    public Activity createActivity() {
        return new Activity();
    }

    /**
     * Create an instance of {@link HeadGesture }
     * 
     */
    public HeadGesture createHeadGesture() {
        return new HeadGesture();
    }

    /**
     * Create an instance of {@link SpeechRate }
     * 
     */
    public SpeechRate createSpeechRate() {
        return new SpeechRate();
    }

    /**
     * Create an instance of {@link SmileFrequency }
     * 
     */
    public SmileFrequency createSmileFrequency() {
        return new SmileFrequency();
    }

    /**
     * Create an instance of {@link TimeStamp }
     * 
     */
    public TimeStamp createTimeStamp() {
        return new TimeStamp();
    }

    /**
     * Create an instance of {@link FaceGaze }
     * 
     */
    public FaceGaze createFaceGaze() {
        return new FaceGaze();
    }

    /**
     * Create an instance of {@link Head }
     * 
     */
    public Head createHead() {
        return new Head();
    }

    /**
     * Create an instance of {@link SpeechFraction }
     * 
     */
    public SpeechFraction createSpeechFraction() {
        return new SpeechFraction();
    }

    /**
     * Create an instance of {@link ArmLeft }
     * 
     */
    public ArmLeft createArmLeft() {
        return new ArmLeft();
    }

    /**
     * Create an instance of {@link Anxiety }
     * 
     */
    public Anxiety createAnxiety() {
        return new Anxiety();
    }

    /**
     * Create an instance of {@link HeadPose }
     * 
     */
    public HeadPose createHeadPose() {
        return new HeadPose();
    }

    /**
     * Create an instance of {@link Agreement }
     * 
     */
    public Agreement createAgreement() {
        return new Agreement();
    }

    /**
     * Create an instance of {@link ArmRight }
     * 
     */
    public ArmRight createArmRight() {
        return new ArmRight();
    }

    /**
     * Create an instance of {@link Layer1 }
     * 
     */
    public Layer1 createLayer1() {
        return new Layer1();
    }

    /**
     * Create an instance of {@link HandGesture }
     * 
     */
    public HandGesture createHandGesture() {
        return new HandGesture();
    }

    /**
     * Create an instance of {@link HandPoseRight }
     * 
     */
    public HandPoseRight createHandPoseRight() {
        return new HandPoseRight();
    }

    /**
     * Create an instance of {@link Source }
     * 
     */
    public Source createSource() {
        return new Source();
    }

    /**
     * Create an instance of {@link Position }
     * 
     */
    public Position createPosition() {
        return new Position();
    }

    /**
     * Create an instance of {@link SpeechVariation }
     * 
     */
    public SpeechVariation createSpeechVariation() {
        return new SpeechVariation();
    }

    /**
     * Create an instance of {@link Presence }
     * 
     */
    public Presence createPresence() {
        return new Presence();
    }

    /**
     * Create an instance of {@link Attention }
     * 
     */
    public Attention createAttention() {
        return new Attention();
    }

    /**
     * Create an instance of {@link FaceGesture }
     * 
     */
    public FaceGesture createFaceGesture() {
        return new FaceGesture();
    }

    /**
     * Create an instance of {@link Body }
     * 
     */
    public Body createBody() {
        return new Body();
    }

    /**
     * Create an instance of {@link Engagement }
     * 
     */
    public Engagement createEngagement() {
        return new Engagement();
    }

    /**
     * Create an instance of {@link Layer2 }
     * 
     */
    public Layer2 createLayer2() {
        return new Layer2();
    }

    /**
     * Create an instance of {@link Pml }
     * 
     */
    public Pml createPml() {
        return new Pml();
    }

    /**
     * Create an instance of {@link Rotation }
     * 
     */
    public Rotation createRotation() {
        return new Rotation();
    }

    /**
     * Create an instance of {@link HandPoseLeft }
     * 
     */
    public HandPoseLeft createHandPoseLeft() {
        return new HandPoseLeft();
    }

    /**
     * Create an instance of {@link Posture }
     * 
     */
    public Posture createPosture() {
        return new Posture();
    }

    /**
     * Create an instance of {@link EyeGaze }
     * 
     */
    public EyeGaze createEyeGaze() {
        return new EyeGaze();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "postureType")
    public JAXBElement<String> createPostureType(String value) {
        return new JAXBElement<String>(_PostureType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "behaviorType")
    public JAXBElement<String> createBehaviorType(String value) {
        return new JAXBElement<String>(_BehaviorType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "presenceType")
    public JAXBElement<String> createPresenceType(String value) {
        return new JAXBElement<String>(_PresenceType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "conf_str")
    public JAXBElement<String> createConfStr(String value) {
        return new JAXBElement<String>(_ConfStr_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "postureStatus")
    public JAXBElement<String> createPostureStatus(String value) {
        return new JAXBElement<String>(_PostureStatus_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "postureDegree")
    public JAXBElement<Double> createPostureDegree(Double value) {
        return new JAXBElement<Double>(_PostureDegree_QNAME, Double.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "headGestureType")
    public JAXBElement<String> createHeadGestureType(String value) {
        return new JAXBElement<String>(_HeadGestureType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "confidence")
    public JAXBElement<Double> createConfidence(Double value) {
        return new JAXBElement<Double>(_Confidence_QNAME, Double.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "gestureValue")
    public JAXBElement<Double> createGestureValue(Double value) {
        return new JAXBElement<Double>(_GestureValue_QNAME, Double.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "gazeCategoryHorizontal")
    public JAXBElement<String> createGazeCategoryHorizontal(String value) {
        return new JAXBElement<String>(_GazeCategoryHorizontal_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "behaviorValue")
    public JAXBElement<Double> createBehaviorValue(Double value) {
        return new JAXBElement<Double>(_BehaviorValue_QNAME, Double.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "gazeCategoryDirection")
    public JAXBElement<String> createGazeCategoryDirection(String value) {
        return new JAXBElement<String>(_GazeCategoryDirection_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "faceGestureType")
    public JAXBElement<String> createFaceGestureType(String value) {
        return new JAXBElement<String>(_FaceGestureType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "handGestureType")
    public JAXBElement<String> createHandGestureType(String value) {
        return new JAXBElement<String>(_HandGestureType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "gazeCategoryVertical")
    public JAXBElement<String> createGazeCategoryVertical(String value) {
        return new JAXBElement<String>(_GazeCategoryVertical_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "armType")
    public JAXBElement<String> createArmType(String value) {
        return new JAXBElement<String>(_ArmType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "poseType")
    public JAXBElement<String> createPoseType(String value) {
        return new JAXBElement<String>(_PoseType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "speechRateValue")
    public JAXBElement<Double> createSpeechRateValue(Double value) {
        return new JAXBElement<Double>(_SpeechRateValue_QNAME, Double.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "speechVariationValue")
    public JAXBElement<Double> createSpeechVariationValue(Double value) {
        return new JAXBElement<Double>(_SpeechVariationValue_QNAME, Double.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "speechFractionValue")
    public JAXBElement<Double> createSpeechFractionValue(Double value) {
        return new JAXBElement<Double>(_SpeechFractionValue_QNAME, Double.class, null, value);
    }

}
