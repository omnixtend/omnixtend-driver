`timescale 1ps/1ps

module AXISTXControlBlackBox (
output	reg	[31:0]						m_axis_txc_tdata           				, 
output	reg	[ 3:0]						m_axis_txc_tkeep           				, 
output	   	      						m_axis_txc_tvalid          				, 
output	   	      						m_axis_txc_tlast           				, 
input               					m_axis_txc_tready          				, 

input              						m_axis_txd_tvalid          				, 
                    					                           				
input               					axis_resetn                				, 
input               					axis_clk
);
// //////////////////////////////////////////////////////////////////////////////
reg 		[15:0] 						txc_cnt									;
reg 		[15:0] 						txc_cnt_int								;

reg        								r_txd_trig								;
reg        								m_axis_txc_tvalid_int					;
reg        								m_axis_txc_tlast_int					;
reg			[ 1:0]						r_txd_tvalid_d							;
// //////////////////////////////////////////////////////////////////////////////
assign m_axis_txc_tlast = m_axis_txc_tlast_int;
assign m_axis_txc_tvalid = m_axis_txc_tvalid_int;

assign txc_last_word 					= (txc_cnt_int >= 6)					;


always @(posedge axis_clk ) 
begin
    if (!axis_resetn) 
    	begin
        r_txd_tvalid_d 					<= 0									;
        r_txd_trig						<= 0									;
    	end 
    else 
    	begin
        r_txd_tvalid_d[0]				<= m_axis_txd_tvalid					;
        r_txd_tvalid_d[1]				<= r_txd_tvalid_d[0]					;
        if	( r_txd_tvalid_d == 2'b01 )
        	r_txd_trig					<= 1									;
        else
        	r_txd_trig					<= 0									;
    	end
end

always @ (*) 	//*/
begin
    txc_cnt_int = txc_cnt;
    if 		( r_txd_trig ) 
    	begin
        txc_cnt_int = 1;
    	end 
    else if ( m_axis_txc_tvalid_int && m_axis_txc_tready && m_axis_txc_tlast_int) 
    	begin
        txc_cnt_int = 0;
    	end 
    else if (m_axis_txc_tvalid_int && m_axis_txc_tready) 
    	begin
        txc_cnt_int = txc_cnt + 1;
    	end
end



always @ (posedge axis_clk) 
begin
    if (!axis_resetn) begin
        m_axis_txc_tdata 			<= 32'h05487B9A;
        m_axis_txc_tvalid_int    	<= 0;
        m_axis_txc_tlast_int     	<= 0;
        m_axis_txc_tkeep     		<= 0;
    end else begin
        m_axis_txc_tvalid_int    	<= (|txc_cnt_int);
        m_axis_txc_tlast_int     	<= txc_last_word;
        m_axis_txc_tkeep     		<= 4'hF;
        case (txc_cnt_int) 
            1: begin
                m_axis_txc_tdata 	<= 32'h1a5aa5a5;
            end
            2: begin
                m_axis_txc_tdata <= 32'h25a55a5a;
            end
            3: begin
                m_axis_txc_tdata <= 32'h3a5aa5a5;
            end
            4: begin
                m_axis_txc_tdata <= 32'h45a55a5a;
            end
            5: begin
                m_axis_txc_tdata <= 32'h5a5aa5a5;
            end
            6: begin
                m_axis_txc_tdata <= 32'h65a55a5a;
            end
            default m_axis_txc_tdata <= m_axis_txc_tdata;
        endcase
    end
end


always @ (posedge axis_clk) 
begin
    if ( !axis_resetn ) 
    	begin
        txc_cnt 						<= 0;
    	end 
	else 
		begin
        txc_cnt 						<= txc_cnt_int;
    	end
end


endmodule 
