package instruction;

import java.util.Objects;

public class Instruction {
    private Operation opt;
    Integer x;

    public Instruction(Operation opt) {
        this.opt = opt;
        this.x = 0;
    }

    public Instruction(Operation opt, Integer x) {
        this.opt = opt;
        this.x = x;
    }

    public Instruction() {
        this.opt = Operation.nop;
        this.x = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Instruction that = (Instruction) o;
        return opt == that.opt && Objects.equals(x, that.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opt, x);
    }

    public Operation getOpt() {
        return opt;
    }

    public void setOpt(Operation opt) {
        this.opt = opt;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }
//    nop,pop,stroe8,load8,addi, subi,muli,divi,ret,cmpi,
//    push,globa,loca, arga,br,brfalse,brtrue, call,
    @Override
    public String toString() {
        switch (this.opt) {
            case nop:
            case pop:
            case stroe64:
            case load64:
            case addi:
            case subi:
            case muli:
            case divi:
            case ret:
            case cmpi:
            case dup:
                return String.format("%s", this.opt);
            case push:
            case globa:
            case loca:
            case br:
            case arga:
            case call:
            case brtrue:
            case brfalse:
                return String.format("%s %s", this.opt, this.x);
            default:
                return "panic";
        }
    }
}
