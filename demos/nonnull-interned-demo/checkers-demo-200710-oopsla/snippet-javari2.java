  \textcolor{orange}{@ReadOnly} Date readonlyDate;
            Date mutableDate;
   
  mutableDate.getTime();
  readonlyDate.getTime();
  
  mutableDate.setTime(time);
  readonlyDate.setTime(time);  \textcolor{red}{// Error: modifies a ReadOnly object!}
