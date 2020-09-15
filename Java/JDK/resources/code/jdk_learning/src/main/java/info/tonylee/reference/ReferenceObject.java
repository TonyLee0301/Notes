package info.tonylee.reference;

public class ReferenceObject {

    private String name;

    private Integer age;

    private SubObject subObject;

    public ReferenceObject(){
    }

    public ReferenceObject(String name){
        this.name = name;
    }

    private ReferenceObject(int age){
       this.age = age;
    }

    public ReferenceObject(String name, SubObject subObject){
        this.name = name;
        this.subObject = subObject;
    }

    protected ReferenceObject(Integer age, SubObject subObject){
        this.age = age;
        this.subObject = subObject;
    }

    public class SubObject{
    }

}

