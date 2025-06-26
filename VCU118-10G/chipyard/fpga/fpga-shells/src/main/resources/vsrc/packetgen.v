module PacketGen(
input	wire							clk										,
input	wire							rst										,
input	wire							test_mode								,
input	wire							m_axis_tready							,
output	wire							m_axis_tvalid							,
output	wire	[63:0]					m_axis_tdata							,
output	wire							m_axis_tlast							,
output	wire	[ 7:0]					m_axis_tkeep							,
output	wire	[13:0]					o_thr_cnt								,
output	wire							o_thr_valid
);
// /////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////
reg										r_axis_tvalid							;
reg		[63:0]							r_axis_tdata							;
reg										r_axis_tlast							;
reg		[ 7:0]							r_axis_tkeep							;

reg		[ 9:0]							r_pkt_cnt								;
reg		[13:0]							tic_cnt									;
reg		[13:0]							thr_cnt									;
reg		[13:0]							thr_cnt_r								;
reg										r_thr_valid								;
// /////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////
assign	m_axis_tvalid					= r_axis_tvalid							;
assign	m_axis_tdata					= r_axis_tdata							;
assign	m_axis_tlast					= r_axis_tlast							;
assign	m_axis_tkeep					= r_axis_tkeep							;

assign	o_thr_cnt						= thr_cnt_r								;
assign	o_thr_valid						= r_thr_valid							;
// /////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////
always@(posedge clk)
begin
	if (!rst)
		begin
		r_axis_tvalid					<= 1'b0									;
		r_axis_tdata					<= 64'h0								;
		r_axis_tlast					<= 1'b0									;
		r_axis_tkeep					<= 8'hff								;
		r_pkt_cnt						<= 10'd0								;
		end
	else
		begin
		if ( test_mode )
			begin
			if (  r_pkt_cnt <= 10'd511 )
				begin
				if ( m_axis_tready )
					begin
					r_axis_tvalid		<= 1'b1									;
					r_pkt_cnt			<= r_pkt_cnt + 10'd1					;
					if ( r_pkt_cnt == 10'd511 )
						r_axis_tlast	<= 1'b1									;
					end
				else
					begin
					r_axis_tvalid		<= 1'b0									;
					r_axis_tlast		<= 1'b0									;
					end
				end
			else
				begin
				r_pkt_cnt				<= 10'd0								;
				r_axis_tvalid			<= 1'b0									;
				r_axis_tlast			<= 1'b0									;
				end
			end
		else
			begin
			r_axis_tvalid				<= 1'b0									;
			r_axis_tdata				<= 64'h0								;
			r_axis_tlast				<= 1'b0									;
			r_pkt_cnt					<= 10'd0								;
			end
		end
end

always@(posedge clk)
begin
	if (!rst)
		begin
		tic_cnt							<= 14'd0								;
		thr_cnt							<= 14'd0								;
		thr_cnt_r						<= 14'd0								;
		r_thr_valid						<= 1'b0									;
		end
	else
		begin
		if (tic_cnt < 14'd9999)
			begin
			r_thr_valid					<= 1'b0									;
			tic_cnt						<= tic_cnt + 14'd1						;
			if (r_axis_tvalid)
				thr_cnt					<= thr_cnt + 14'd1						;
			end
		else if (tic_cnt == 14'd9999)
			begin
			tic_cnt						<= 14'd0								;
			thr_cnt_r					<= thr_cnt								;
			thr_cnt						<= 14'd0								;
			r_thr_valid					<= 1'b1									;
			end
		end
end

endmodule