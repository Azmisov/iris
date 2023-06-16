package us.mn.state.dot.tms.server.comm.ntcip;

// TODO: just make this a Json interface?
abstract public class EssValues {
    /** Serialize the object to a debugging/log string */
    abstract public String toString();
    /** Serialize the object to a Json string */
    abstract public String toJson();
}
