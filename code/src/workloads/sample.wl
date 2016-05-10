init create_network Star 3
init enable_service Ajil
init set_quota_manager 3
init set_global_limit 205
+0 init_services
+10 join_group 1 1
+0 join_group 2 1
+0 join_group 3 1
+0 report_ajil_w_freq 100
+10 send_mcast 1 1 10
+0 send_mcast 2 1 100
+0 send_mcast 3 1 100
+0 nop
#+0 runall
+400 stop