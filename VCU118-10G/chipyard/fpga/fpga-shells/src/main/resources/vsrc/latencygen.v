module LatencyGen(
input	wire							clk										,
input	wire							rst										,
input	wire							test_mode								,
input	wire							m_axis_tready							,
input	wire							s_axis_tlast							,
input	wire							m_axis_tlast							,
output	wire	[13:0]					o_lat_cnt								,
output	wire 							o_lat_valid
);
// /////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////
reg		[ 9:0]							r_pkt_cnt								;
reg		[13:0]							r_lat_cnt								;
reg		[13:0]							r_lat_cnt_r								;
reg										r_lat_valid								;

reg		[ 1:0]							r_state_lat								;

reg										m_axis_tlast_d							;
reg										s_axis_tlast_d							;

localparam 	LAT_IDLE					= 2'd0 									;
localparam 	LAT_FIND					= 2'd1 									;
localparam 	LAT_DONE					= 2'd2 									;
// /////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////
assign	o_lat_cnt						= r_lat_cnt_r							;
assign	o_lat_valid						= r_lat_valid							;
// /////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////
always@(posedge clk)
begin
	if (!rst)
		begin
		m_axis_tlast_d					<= 1'b0									;
		s_axis_tlast_d					<= 1'b0									;
		end
	else
		begin
		m_axis_tlast_d					<= m_axis_tlast							;
		s_axis_tlast_d					<= s_axis_tlast							;
		end
end

always@(posedge clk)
begin
	if (!rst)
		begin
		r_state_lat					 	<= 2'd0									;
		r_lat_cnt						<= 14'd0								;
		r_lat_cnt_r						<= 14'd0								;
		r_lat_valid						<= 1'b0									;
		end
	else
		begin
		case	( r_state_lat )
		LAT_IDLE :
			begin
			if ( test_mode && ( m_axis_tlast && !m_axis_tlast_d ) )
				begin
				r_lat_cnt				<= r_lat_cnt + 14'd1					;
				r_state_lat				<= LAT_FIND								;
				end
			else
				r_state_lat				<= LAT_IDLE								;
			end
		LAT_FIND :
			begin
			if ( s_axis_tlast && !s_axis_tlast_d )
				begin
				r_lat_cnt_r				<= r_lat_cnt							;
				r_lat_valid				<= 1'b1									;
				r_state_lat				<= LAT_DONE								;
				end
			else
				begin
				r_lat_cnt				<= r_lat_cnt + 14'd1					;
				r_state_lat				<= LAT_FIND								;
				end
			end
		LAT_DONE :
			begin
			r_lat_cnt					<= 14'd0								;
			r_lat_valid					<= 1'b0									;
			r_state_lat					<= LAT_IDLE								;
			end
		default :
			r_state_lat					<= 2'd0									;
		endcase
		end
end

endmodule