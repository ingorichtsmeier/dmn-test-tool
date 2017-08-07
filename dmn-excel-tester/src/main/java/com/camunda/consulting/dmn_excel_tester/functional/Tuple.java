package com.camunda.consulting.dmn_excel_tester.functional;

public class Tuple <T, U> {
  public final T _1;
  public final U _2;
  
  public Tuple(T t, U u) {
    this._1 = t;
    this._2 = u;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Tuple<?, ?>) {   
      return ((Tuple<?, ?>) obj)._1.equals(this._1) && (((Tuple<?, ?>) obj)._2.equals(this._2)) ? true : false ;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return _1 + ":" + _2;
  }
}
