package com.matburt.mobileorg;
import android.content.res.Resources.NotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class SynchronizerProxy implements Synchronizer
{
    Method push;
    Method pull;
    Method close;
    Object inst;
    SynchronizerProxy(Object inst) throws NoSuchMethodException
    {
        this.inst = inst;
        push = inst.getClass().getMethod("push");
        pull = inst.getClass().getMethod("pull");
        close = inst.getClass().getMethod("close");
    }
    public void pull() throws NotFoundException, ReportableError
    {
        try {
            pull.invoke(inst, new Object[0]);
        }
        catch(IllegalAccessException ex)
        {
        }
        catch(InvocationTargetException ex)
        {
        }

    }
    public void push() throws NotFoundException, ReportableError
    {
        try {
            push.invoke(inst, new Object[0]);
        }
        catch(IllegalAccessException ex)
        {
        }
        catch(InvocationTargetException ex)
        {
        }
    }
    public void close()
    {
        try {
            close.invoke(inst, new Object[0]);
        }
        catch(IllegalAccessException ex)
        {
        }
        catch(InvocationTargetException ex)
        {
        }
    }
}
