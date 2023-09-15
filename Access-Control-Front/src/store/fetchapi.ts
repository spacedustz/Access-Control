import axios from "axios";

export const fetchTripwireData = async (): Promise<any[]> => {
    try {
        const response = await axios.get('http://localhost:3031/count');
        return response.data;
    } catch (error) {
        console.error('TripWire 데이터 불러오기 실패: ', error);
        return [];
    }
}