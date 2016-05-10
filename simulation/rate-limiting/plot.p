set term png
#set term post eps enhanced

#set xtic auto
#set ytic auto
set pointsize 2.5

set grid
set border 3 # Remove border on top and right.  These
# borders are useless and make it harder
# to see plotted lines near the border.
set xtics nomirror
set ytics nomirror

#set log x
#set mxtics 10    # Makes logscale look good

# Line styles: try to pick pleasing colors, rather
# than strictly primary colors or hard-to-see colors
# like gnuplot's default yellow.  Make the lines thick
# so they're easy to see in small plots in papers.
#set style line 1 lt rgb "#A00000" lw 2 pt 1
#set style line 2 lt rgb "#00A000" lw 2 pt 6
#set style line 3 lt rgb "#5060D0" lw 2 pt 2
#set style line 4 lt rgb "#F25900" lw 2 pt 9

set style line 1 lt 9 lw 2.5
set style fill solid 0.5 border

set output "ajil-peek.png"
set title "Multicast Traffic with Ajil Rate-Limiting"
set xlabel "Time (s)"
set ylabel "Traffic (KB)"
set yr[0:2000]
set xr[0:1000]

plot "peek/data.csv" using 1:2 every 1 title "IPMC no AJIL" with line lw 1.5, \
      "peek/data.csv" using 1:3 every 1 title "AJIL" with line lw 1.5, \
      "peek/data.csv" using 1:4 every 1 title "Desired Limit" with l ls 1, \
      "peek/data.csv" using 1:5 every 1 title "Broadcast Traffic" with boxes
