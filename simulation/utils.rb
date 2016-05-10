def fmod(a,b)
   return (a % b) == 0
   if b == 0
      return false
   else
      return (a/b) == (a.to_f / b.to_f)
   end
end

def avg_and_sdev(values)
   average = 0.0
   std_dev = 0.0

   # convert all the array elements to floats
   values.map!{ |v| v.to_f }

   sum = 0.0
   values.each{ |v| sum += v }

   average = sum / values.length

   sqr_dev = 0.0  # sum_{i=1}^{N} (x_i - avg)^2
   values.each{ |v| sqr_dev += (v - average) * (v - average) }

   std_dev = Math.sqrt(sqr_dev / values.length)

   return average, std_dev
end

def to_next_i(a)
   if a == a.to_i
      return a.to_i
   else
      return a.to_i + 1
   end
end
