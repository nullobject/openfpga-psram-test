#
# user core constraints
#
# put your clock groups in here as well as any net assignments
#

create_generated_clock -name cram_clk \
  -source [get_pins {ic|mp1|mf_pllbase_inst|altera_pll_i|general[1].gpll~PLL_OUTPUT_COUNTER|divclk}] \
  [get_ports {cram0_clk}]

derive_clock_uncertainty

set_clock_groups -asynchronous \
 -group {bridge_spiclk} \
 -group {clk_74a} \
 -group {clk_74b} \
 -group {ic|mp1|mf_pllbase_inst|altera_pll_i|general[*].gpll~PLL_OUTPUT_COUNTER|divclk}

set_input_delay -clock cram_clk -max 7 [get_ports {cram0_wait cram0_dq[*]}]
set_input_delay -clock cram_clk -min 2 [get_ports {cram0_wait cram0_dq[*]}]

set_output_delay -clock cram_clk -max 3 [get_ports {cram0_cre cram0_ce0_n cram1_ce1_n cram0_adv_n cram0_oe_n cram0_we_n cram0_ub_n cram0_lb_n cram0_a[*] cram0_dq[*]}]
set_output_delay -clock cram_clk -min -2 [get_ports {cram0_cre cram0_ce0_n cram1_ce1_n cram0_adv_n cram0_oe_n cram0_we_n cram0_ub_n cram0_lb_n cram0_a[*] cram0_dq[*]}]

set_multicycle_path -from [get_clocks {cram_clk}] -to [get_clocks {ic|mp1|mf_pllbase_inst|altera_pll_i|general[0].gpll~PLL_OUTPUT_COUNTER|divclk}] -setup -end 2
